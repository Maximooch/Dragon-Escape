import * as pc from "playcanvas";
import { Client, type Room } from "@colyseus/sdk";
import { COURSE, progressAt, type CourseBlock } from "../course";
import {
  createRunner,
  destroyNearDragon,
  dragonPosition,
  EMPTY_INPUT,
  simulateRunner,
  type InputState,
  type RunnerState,
  type Vec3,
} from "../simulation";
import { beginPointerLockedJoin } from "./input";
import { unlockAudioContext } from "./audio";

export type GameView = {
  phase: "menu" | "countdown" | "racing" | "results";
  banner: string;
  time: number;
  progress: number;
  place: number;
  playerCount: number;
  checkpoint: string;
  leapCooldown: number;
  online: boolean;
  eliminated: boolean;
  pointerLocked: boolean;
  audioState: "idle" | "ready" | "blocked" | "muted";
};

type Rival = { id: string; name: string; speed: number; offset: number; entity: pc.Entity; finished: boolean };
type Snapshot = {
  phase: GameView["phase"];
  countdown: number;
  elapsed: number;
  dragonZ: number;
  destroyed: string[];
  players: Array<{ id: string; name: string; position: Vec3; alive: boolean; finished: boolean }>;
};

const rivalNames = ["Moth", "Bramble", "Kite", "Rook", "Vesper"];

export class DragonEscapeRuntime {
  private app: pc.Application | null = null;
  private camera!: pc.Entity;
  private dragon!: pc.Entity;
  private canvas: HTMLCanvasElement;
  private onView: (view: GameView) => void;
  private runner: RunnerState = createRunner();
  private input: InputState = { ...EMPTY_INPUT };
  private keys = new Set<string>();
  private yaw = 0;
  private pitch = -0.08;
  private phase: GameView["phase"] = "menu";
  private elapsed = 0;
  private countdown = 0;
  private resultHold = 0;
  private destroyed = new Set<string>();
  private blockEntities = new Map<string, pc.Entity>();
  private rivals: Rival[] = [];
  private room: Room | null = null;
  private online = false;
  private latestSnapshot: Snapshot | null = null;
  private inputAccumulator = 0;
  private lastViewUpdate = 0;
  private leapWasDown = false;
  private name = "Runner";
  private muted = false;
  private audio: AudioContext | null = null;
  private audioState: GameView["audioState"] = "idle";
  private media = new Map<"check" | "roar" | "wing", HTMLAudioElement>();
  private lastCountdownBeat = 0;
  private nextDragonGrowl = 2;
  private nextWingBeat = 0;

  constructor(canvas: HTMLCanvasElement, onView: (view: GameView) => void) {
    this.canvas = canvas;
    this.onView = onView;
  }

  mount() {
    const app = new pc.Application(this.canvas, {
      keyboard: new pc.Keyboard(window),
      mouse: new pc.Mouse(this.canvas),
      graphicsDeviceOptions: { alpha: false, antialias: true },
    });
    this.app = app;
    app.setCanvasFillMode(pc.FILLMODE_FILL_WINDOW);
    app.setCanvasResolution(pc.RESOLUTION_AUTO);
    app.start();
    this.buildWorld();
    this.bindInput();
    app.on("update", this.update, this);
    window.addEventListener("resize", this.resize);
    this.resize();
    this.emitView(true);
  }

  private resize = () => this.app?.resizeCanvas();

  destroy() {
    window.removeEventListener("resize", this.resize);
    window.removeEventListener("keydown", this.keyDown);
    window.removeEventListener("keyup", this.keyUp);
    document.removeEventListener("mousemove", this.mouseMove);
    document.removeEventListener("pointerlockchange", this.pointerLockChange);
    document.removeEventListener("visibilitychange", this.visibilityChange);
    for (const element of this.media.values()) {
      element.pause();
      element.removeAttribute("src");
      element.load();
    }
    this.media.clear();
    void this.audio?.close();
    this.audio = null;
    this.room?.leave();
    this.app?.destroy();
    this.app = null;
  }

  capturePointer() {
    if (this.phase === "menu" || document.pointerLockElement === this.canvas) return;
    const request = this.canvas.requestPointerLock?.();
    if (request && "catch" in request) request.catch(() => undefined);
  }

  setMuted(muted: boolean) {
    this.muted = muted;
    if (muted) {
      for (const element of this.media.values()) element.pause();
      this.setAudioState("muted");
    } else {
      this.testSound();
    }
  }

  private unlockWebAudio() {
    if (this.muted) return null;
    this.audio = unlockAudioContext(
      this.audio,
      () => new AudioContext(),
      () => this.setAudioState("blocked"),
    );
    this.audio.onstatechange = () => {
      if (this.muted) this.setAudioState("muted");
      else if (this.audio?.state === "running") this.setAudioState("ready");
      else if (this.audio?.state === "suspended") this.setAudioState("blocked");
    };
    return this.audio;
  }

  private setAudioState(state: GameView["audioState"]) {
    if (this.audioState === state) return;
    this.audioState = state;
    this.emitView(true);
  }

  private mediaElement(kind: "check" | "roar" | "wing") {
    const existing = this.media.get(kind);
    if (existing) return existing;
    const urls = {
      check: "/audio/audio-check.wav",
      roar: "/audio/dragon-roar.wav",
      wing: "/audio/wing-beat.wav",
    };
    const element = new Audio(urls[kind]);
    element.preload = "auto";
    this.media.set(kind, element);
    return element;
  }

  private playMedia(kind: "check" | "roar" | "wing", volume: number) {
    if (this.muted) return Promise.resolve();
    const element = this.mediaElement(kind);
    element.pause();
    element.currentTime = 0;
    element.volume = Math.max(0, Math.min(1, volume));
    const playback = element.play();
    return playback.then(() => this.setAudioState("ready")).catch(() => {
      this.setAudioState("blocked");
      throw new Error("audio playback blocked");
    });
  }

  testSound() {
    if (this.muted) return;
    this.unlockWebAudio();
    void this.playMedia("check", 0.72).catch(() => undefined);

    // Authorize the reusable creature elements inside the same user gesture.
    for (const kind of ["roar", "wing"] as const) {
      const element = this.mediaElement(kind);
      element.muted = true;
      void element.play().then(() => {
        element.pause();
        element.currentTime = 0;
        element.muted = false;
      }).catch(() => { element.muted = false; });
    }
  }

  private tone(frequency: number, duration = 0.08, volume = 0.025) {
    if (this.muted) return;
    const audio = this.unlockWebAudio();
    if (!audio) return;
    const oscillator = audio.createOscillator();
    const gain = audio.createGain();
    oscillator.type = "sawtooth";
    oscillator.frequency.value = frequency;
    gain.gain.setValueAtTime(volume, audio.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.0001, audio.currentTime + duration);
    oscillator.connect(gain).connect(audio.destination);
    oscillator.start();
    oscillator.stop(audio.currentTime + duration);
  }

  private dragonGrowl(distance: number) {
    if (this.muted) return;
    const proximity = Math.max(0.15, Math.min(1, 1 - distance / 85));
    void this.playMedia("roar", 0.28 + proximity * 0.62).catch(() => {
      this.tone(150, 0.8, 0.12 * proximity);
    });
  }

  private wingBeat(distance: number) {
    if (this.muted || distance > 48) return;
    const proximity = Math.max(0.1, 1 - distance / 60);
    void this.playMedia("wing", 0.12 + proximity * 0.42).catch(() => undefined);
  }

  private updateDragonAudio(dragonZ: number) {
    if (this.phase !== "racing") return;
    const distance = Math.abs(this.runner.position.z - dragonZ);
    if (this.elapsed >= this.nextDragonGrowl) {
      this.dragonGrowl(distance);
      this.nextDragonGrowl = this.elapsed + 3.5 + Math.min(4, distance * 0.035);
    }
    if (this.elapsed >= this.nextWingBeat) {
      this.wingBeat(distance);
      this.nextWingBeat = this.elapsed + 1.05;
    }
  }

  join(name: string) {
    this.name = name;
    this.testSound();
    // Safari requires pointer lock to stay inside the original button gesture.
    beginPointerLockedJoin(
      () => this.resetRace(),
      () => this.capturePointer(),
      () => { void this.connectToRoom(name); },
    );
  }

  private async connectToRoom(name: string) {
    try {
      const endpoint = process.env.NEXT_PUBLIC_GAME_SERVER_URL || "ws://localhost:2567";
      const client = new Client(endpoint);
      const room = await Promise.race([
        client.joinOrCreate("dragon_escape", { name }),
        new Promise<never>((_, reject) => setTimeout(() => reject(new Error("offline")), 900)),
      ]);
      this.room = room;
      this.online = true;
      room.onMessage("snapshot", (snapshot: Snapshot) => { this.latestSnapshot = snapshot; });
      room.onLeave(() => { this.online = false; this.room = null; });
    } catch {
      this.online = false;
      this.phase = "countdown";
      this.countdown = 3.8;
    }
    this.emitView(true);
  }

  private resetRace() {
    this.runner = createRunner();
    this.destroyed.clear();
    for (const [id, entity] of this.blockEntities) entity.enabled = !this.destroyed.has(id);
    this.elapsed = 0;
    this.resultHold = 0;
    this.latestSnapshot = null;
    this.phase = "countdown";
    this.countdown = 3.8;
    this.lastCountdownBeat = 0;
    this.nextDragonGrowl = 2;
    this.nextWingBeat = 0;
    this.rivals.forEach((rival) => { rival.finished = false; rival.entity.enabled = true; });
  }

  private bindInput() {
    window.addEventListener("keydown", this.keyDown);
    window.addEventListener("keyup", this.keyUp);
    document.addEventListener("mousemove", this.mouseMove);
    document.addEventListener("pointerlockchange", this.pointerLockChange);
    document.addEventListener("visibilitychange", this.visibilityChange);
  }

  private pointerLockChange = () => this.emitView(true);
  private visibilityChange = () => {
    if (document.visibilityState === "visible" && !this.muted) this.unlockWebAudio();
  };

  private keyDown = (event: KeyboardEvent) => {
    this.keys.add(event.code);
    if (["Space", "ArrowUp", "ArrowDown"].includes(event.code)) event.preventDefault();
  };
  private keyUp = (event: KeyboardEvent) => this.keys.delete(event.code);
  private mouseMove = (event: MouseEvent) => {
    if (document.pointerLockElement !== this.canvas) return;
    this.yaw -= event.movementX * 0.00215;
    this.pitch = Math.max(-1.35, Math.min(1.2, this.pitch - event.movementY * 0.0019));
  };

  private buildWorld() {
    const app = this.app!;
    const camera = new pc.Entity("RunnerCamera");
    camera.addComponent("camera", { clearColor: new pc.Color(0.025, 0.02, 0.045), farClip: 230, fov: 76 });
    app.root.addChild(camera);
    this.camera = camera;

    const sun = new pc.Entity("Moonlight");
    sun.addComponent("light", { type: "directional", color: new pc.Color(0.62, 0.72, 1), intensity: 1.7, castShadows: true });
    sun.setEulerAngles(52, 28, 0);
    app.root.addChild(sun);
    app.scene.ambientLight = new pc.Color(0.18, 0.16, 0.27);

    const materials = {
      basalt: this.material(new pc.Color(0.09, 0.08, 0.12), new pc.Color(0.04, 0.02, 0.06)),
      sandstone: this.material(new pc.Color(0.52, 0.37, 0.24), new pc.Color(0.06, 0.025, 0.01)),
      ember: this.material(new pc.Color(0.33, 0.12, 0.055), new pc.Color(1, 0.12, 0.015)),
      finish: this.material(new pc.Color(0.17, 0.36, 0.38), new pc.Color(0.03, 0.8, 0.68)),
    };
    for (const b of COURSE.blocks) {
      if (b.visible === false) continue;
      const entity = this.box(b, materials[b.kind]);
      this.blockEntities.set(b.id, entity);
    }

    const lava = new pc.Entity("CinderSea");
    lava.addComponent("render", { type: "box", material: this.material(new pc.Color(0.18, 0.015, 0.01), new pc.Color(1, 0.055, 0.005)) });
    lava.setLocalScale(180, 0.35, 330);
    lava.setPosition(40, -18.5, 110);
    app.root.addChild(lava);

    for (let i = 0; i < 48; i++) {
      const ember = new pc.Entity(`ember-${i}`);
      ember.addComponent("render", { type: "sphere", material: materials.ember });
      const size = 0.025 + (i % 5) * 0.018;
      ember.setLocalScale(size, size, size);
      ember.setPosition(((i * 17) % 145) - 25, 2 + ((i * 7) % 45), (i * 23) % 235);
      app.root.addChild(ember);
    }

    this.dragon = this.createDragon();
    this.rivals = rivalNames.map((name, index) => ({
      id: `bot-${index}`,
      name,
      speed: 4.8 + index * 0.24,
      offset: index * 0.55,
      entity: this.createRival(index),
      finished: false,
    }));
    this.loadPrototypeModels();
    this.camera.setPosition(0, 1.62, -4);
  }

  private loadContainer(url: string, onLoad: (resource: pc.ContainerResource) => void) {
    const app = this.app;
    if (!app) return;
    app.assets.loadFromUrl(url, "container", (error, asset) => {
      if (error || !asset?.resource || this.app !== app) return;
      onLoad(asset.resource as pc.ContainerResource);
    });
  }

  private loadPrototypeModels() {
    this.loadContainer(COURSE.model, (resource) => {
      const environment = resource.instantiateRenderEntity({ castShadows: false, receiveShadows: true });
      environment.name = `${COURSE.name} schematic environment`;
      const transform = COURSE.modelTransform;
      environment.setLocalScale(transform.scale, transform.scale, transform.scale);
      environment.setEulerAngles(0, transform.yaw, 0);
      environment.setPosition(transform.x, transform.y, transform.z);
      this.app?.root.addChild(environment);
    });

    this.loadContainer("/models/cinder-wyrm.glb", (resource) => {
      const model = resource.instantiateRenderEntity({ castShadows: true });
      model.name = "The Cinder Wyrm";
      this.dragon.destroy();
      this.dragon = model;
      this.app?.root.addChild(model);
    });

    this.loadContainer("/models/runner.glb", (resource) => {
      this.rivals.forEach((rival) => {
        const model = resource.instantiateRenderEntity({ castShadows: true });
        model.setLocalPosition(0, -0.66, 0);
        model.setLocalEulerAngles(0, 180, 0);
        rival.entity.removeComponent("render");
        rival.entity.addChild(model);
      });
    });
  }

  private material(diffuse: pc.Color, emissive: pc.Color) {
    const material = new pc.StandardMaterial();
    material.diffuse = diffuse;
    material.emissive = emissive;
    material.metalness = 0.05;
    material.gloss = 0.25;
    material.update();
    return material;
  }

  private box(block: CourseBlock, material: pc.Material) {
    const entity = new pc.Entity(block.id);
    entity.addComponent("render", { type: "box", material, castShadows: true, receiveShadows: true });
    entity.setLocalScale(block.sx, block.sy, block.sz);
    entity.setPosition(block.x, block.y, block.z);
    this.app!.root.addChild(entity);
    return entity;
  }

  private createDragon() {
    const root = new pc.Entity("The Cinder Wyrm");
    const dark = this.material(new pc.Color(0.055, 0.03, 0.035), new pc.Color(0.08, 0.004, 0.002));
    const fire = this.material(new pc.Color(0.32, 0.02, 0.01), new pc.Color(1, 0.035, 0.003));
    const part = (name: string, scale: number[], position: number[], material = dark) => {
      const e = new pc.Entity(name);
      e.addComponent("render", { type: "box", material, castShadows: true });
      e.setLocalScale(scale[0], scale[1], scale[2]);
      e.setLocalPosition(position[0], position[1], position[2]);
      root.addChild(e);
      return e;
    };
    part("head", [3.2, 1.8, 4.2], [0, 0, 2]);
    part("jaw", [2.5, 0.6, 2.8], [0, -0.8, 3.2], fire);
    part("body", [4.6, 3.2, 7], [0, 0.3, -3]);
    part("left-wing", [10, 0.25, 5], [-6, 1, -3]).setLocalEulerAngles(0, 0, -12);
    part("right-wing", [10, 0.25, 5], [6, 1, -3]).setLocalEulerAngles(0, 0, 12);
    part("tail", [1.4, 1.4, 9], [0, 0, -10]);
    part("eye-left", [0.3, 0.3, 0.3], [-0.8, 0.35, 4.15], fire);
    part("eye-right", [0.3, 0.3, 0.3], [0.8, 0.35, 4.15], fire);
    this.app!.root.addChild(root);
    return root;
  }

  private createRival(index: number) {
    const colors = [new pc.Color(0.2, 0.7, 0.75), new pc.Color(0.9, 0.42, 0.18), new pc.Color(0.63, 0.35, 0.86), new pc.Color(0.85, 0.74, 0.24), new pc.Color(0.35, 0.78, 0.4)];
    const entity = new pc.Entity(`rival-${index}`);
    entity.addComponent("render", { type: "capsule", material: this.material(colors[index], colors[index].clone().mulScalar(0.08)) });
    entity.setLocalScale(0.7, 0.85, 0.7);
    this.app!.root.addChild(entity);
    return entity;
  }

  private update(dtRaw: number) {
    const dt = Math.min(dtRaw, 0.05);
    this.updateInput();
    if (this.online && this.latestSnapshot) this.applySnapshot(dt);
    else this.updateOffline(dt);
    this.updateCamera(dt);
    this.emitView(false);
  }

  private updateInput() {
    this.input = {
      forward: this.keys.has("KeyW") || this.keys.has("ArrowUp"),
      back: this.keys.has("KeyS") || this.keys.has("ArrowDown"),
      left: this.keys.has("KeyA"),
      right: this.keys.has("KeyD"),
      jump: this.keys.has("Space"),
      sprint: this.keys.has("ShiftLeft") || this.keys.has("ShiftRight"),
      leap: this.keys.has("KeyQ") && !this.leapWasDown,
      yaw: this.yaw,
    };
    this.leapWasDown = this.keys.has("KeyQ");
  }

  private updateOffline(dt: number) {
    if (this.phase === "menu" || this.phase === "results") return;
    if (this.phase === "countdown") {
      this.countdown -= dt;
      const beat = Math.ceil(this.countdown);
      if (beat > 0 && beat <= 3 && beat !== this.lastCountdownBeat) {
        this.lastCountdownBeat = beat;
        this.tone(beat === 1 ? 720 : 480, 0.11, 0.095);
      }
      if (this.countdown <= 0) { this.phase = "racing"; this.elapsed = 0; }
      return;
    }
    this.elapsed += dt;
    simulateRunner(this.runner, this.input, dt, this.elapsed, this.destroyed);
    const dragonZ = dragonPosition(this.elapsed);
    destroyNearDragon(this.destroyed, dragonZ);
    this.syncDestroyed();
    this.updateDragon(dragonZ, this.elapsed);
    this.updateDragonAudio(dragonZ);
    this.updateRivals(this.elapsed);
    if (this.runner.alive && dragonZ > this.runner.position.z - 2.4) this.runner.alive = false;
    if (!this.runner.alive || this.runner.finished) {
      this.resultHold += dt;
      if (this.resultHold > (this.runner.alive ? 0.7 : 3.2)) this.phase = "results";
    }
  }

  private applySnapshot(dt: number) {
    const snapshot = this.latestSnapshot!;
    this.phase = snapshot.phase;
    this.countdown = snapshot.countdown;
    this.elapsed = snapshot.elapsed;
    this.destroyed = new Set(snapshot.destroyed);
    this.syncDestroyed();
    this.updateDragon(snapshot.dragonZ, snapshot.elapsed);
    this.updateDragonAudio(snapshot.dragonZ);
    const self = snapshot.players.find((player) => player.id === this.room?.sessionId);
    if (self) {
      if (snapshot.phase === "racing") {
        simulateRunner(this.runner, this.input, dt, snapshot.elapsed, this.destroyed);
      }
      const blend = Math.min(1, dt * 10);
      this.runner.position.x += (self.position.x - this.runner.position.x) * blend;
      this.runner.position.y += (self.position.y - this.runner.position.y) * blend;
      this.runner.position.z += (self.position.z - this.runner.position.z) * blend;
      this.runner.alive = self.alive;
      this.runner.finished = self.finished;
    }
    snapshot.players.filter((player) => player.id !== this.room?.sessionId).slice(0, this.rivals.length).forEach((player, index) => {
      const entity = this.rivals[index].entity;
      entity.enabled = player.alive && !player.finished;
      entity.setPosition(player.position.x, player.position.y + 0.85, player.position.z);
    });
    this.inputAccumulator += dt;
    if (this.inputAccumulator >= 1 / 20) {
      this.inputAccumulator = 0;
      this.room?.send("input", this.input);
    }
  }

  private updateDragon(z: number, elapsed: number) {
    const route = this.botPath(Math.max(0, z));
    this.dragon.setPosition(
      route.x + Math.sin(elapsed * 0.65) * 1.4,
      route.y + 4.3 + Math.sin(elapsed * 1.7) * 0.45,
      z,
    );
    const left = this.dragon.findByName("left-wing");
    const right = this.dragon.findByName("right-wing");
    const flap = Math.sin(elapsed * 5.5) * 20;
    left?.setLocalEulerAngles(0, 0, -12 + flap);
    right?.setLocalEulerAngles(0, 0, 12 - flap);
  }

  private updateRivals(elapsed: number) {
    for (const rival of this.rivals) {
      const z = Math.min(COURSE.finishZ + 2, Math.max(0, (elapsed - rival.offset) * rival.speed));
      const p = this.botPath(z);
      rival.entity.setPosition(p.x, p.y + 0.85, p.z);
      rival.entity.enabled = z < COURSE.finishZ;
      rival.finished = z >= COURSE.finishZ;
    }
  }

  private botPath(z: number): Vec3 {
    let previous = COURSE.botPath[0];
    for (const next of COURSE.botPath.slice(1)) {
      if (z <= next.z) {
        const t = Math.max(0, (z - previous.z) / Math.max(0.1, next.z - previous.z));
        return {
          x: previous.x + (next.x - previous.x) * t,
          y: previous.y + (next.y - previous.y) * t + Math.sin(t * Math.PI) * 1.4,
          z,
        };
      }
      previous = next;
    }
    return { ...COURSE.botPath.at(-1)!, z };
  }

  private syncDestroyed() {
    for (const [id, entity] of this.blockEntities) entity.enabled = !this.destroyed.has(id);
  }

  private updateCamera(dt: number) {
    if (!this.camera) return;
    let target = { ...this.runner.position };
    if (!this.runner.alive && this.phase !== "results") {
      const leading = this.rivals.find((rival) => rival.entity.enabled);
      if (leading) { const p = leading.entity.getPosition(); target = { x: p.x, y: p.y, z: p.z }; }
    }
    const bob = this.runner.grounded && (this.input.forward || this.input.back || this.input.left || this.input.right)
      ? Math.sin(performance.now() * 0.012) * 0.035 : 0;
    const desired = new pc.Vec3(target.x, target.y + 1.58 + bob, target.z);
    const current = this.camera.getPosition();
    current.lerp(current, desired, Math.min(1, dt * 18));
    this.camera.setPosition(current);
    this.camera.setEulerAngles(this.pitch * 180 / Math.PI, this.yaw * 180 / Math.PI + 180, 0);
  }

  private emitView(force: boolean) {
    const now = performance.now();
    if (!force && now - this.lastViewUpdate < 80) return;
    this.lastViewUpdate = now;
    const progress = progressAt(this.runner.position.z);
    const rivalProgress = this.rivals.map((rival) => progressAt(rival.entity.getPosition().z));
    const place = 1 + rivalProgress.filter((value) => value > progress).length;
    let banner = "";
    if (this.phase === "countdown") banner = this.countdown > 3 ? "GET READY" : this.countdown > 0 ? String(Math.ceil(this.countdown)) : "RUN";
    else if (this.phase === "racing" && this.elapsed < 1) banner = "RUN";
    else if (!this.runner.alive) banner = "THE DRAGON HAS YOU";
    const checkpoint = COURSE.checkpoints[Math.min(this.runner.checkpoint, COURSE.checkpoints.length - 1)]?.label || "The Gate";
    this.onView({
      phase: this.phase,
      banner,
      time: this.elapsed,
      progress,
      place,
      playerCount: this.online ? Math.max(1, this.latestSnapshot?.players.length || 1) : 6,
      checkpoint,
      leapCooldown: Math.max(0, this.runner.leapReadyAt - this.elapsed),
      online: this.online,
      eliminated: !this.runner.alive,
      pointerLocked: document.pointerLockElement === this.canvas,
      audioState: this.audioState,
    });
  }
}
