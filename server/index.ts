import { Server, Room, type Client } from "colyseus";
import { WebSocketTransport } from "@colyseus/ws-transport";
import { COURSE } from "../lib/course";
import {
  createRunner,
  destroyNearDragon,
  dragonPosition,
  EMPTY_INPUT,
  simulateRunner,
  type InputState,
  type RunnerState,
} from "../lib/simulation";

type PublicRunner = {
  name: string;
  state: RunnerState;
  input: InputState;
};

class DragonEscapeRoom extends Room {
  maxClients = 12;
  private runners = new Map<string, PublicRunner>();
  private phase: "countdown" | "racing" | "results" = "countdown";
  private countdown = 4;
  private elapsed = 0;
  private resultsTime = 0;
  private destroyed = new Set<string>();

  onCreate() {
    this.setSimulationInterval((deltaMs) => this.tick(Math.min(deltaMs / 1000, 0.05)), 1000 / 30);
    this.onMessage("input", (client, input: Partial<InputState>) => {
      const runner = this.runners.get(client.sessionId);
      if (!runner) return;
      runner.input = {
        forward: Boolean(input.forward), back: Boolean(input.back),
        left: Boolean(input.left), right: Boolean(input.right),
        jump: Boolean(input.jump), sprint: Boolean(input.sprint), leap: Boolean(input.leap),
        yaw: Number.isFinite(input.yaw) ? Math.max(-1000, Math.min(1000, Number(input.yaw))) : 0,
      };
    });
  }

  onJoin(client: Client, options: { name?: string }) {
    const state = createRunner();
    if (this.phase === "racing") state.alive = false;
    this.runners.set(client.sessionId, {
      name: String(options?.name || "Runner").slice(0, 18),
      state,
      input: { ...EMPTY_INPUT },
    });
    this.broadcastSnapshot();
  }

  onLeave(client: Client) {
    this.runners.delete(client.sessionId);
  }

  private tick(dt: number) {
    if (this.phase === "countdown") {
      this.countdown -= dt;
      if (this.countdown <= 0) {
        this.phase = "racing";
        this.elapsed = 0;
      }
    } else if (this.phase === "racing") {
      this.elapsed += dt;
      const dragonZ = dragonPosition(this.elapsed);
      destroyNearDragon(this.destroyed, dragonZ);
      for (const runner of this.runners.values()) {
        simulateRunner(runner.state, runner.input, dt, this.elapsed, this.destroyed);
        runner.input.leap = false;
        if (runner.state.alive && dragonZ > runner.state.position.z - 2.4) runner.state.alive = false;
      }
      const active = [...this.runners.values()].filter((runner) => runner.state.alive && !runner.state.finished);
      if ((this.runners.size > 0 && active.length === 0) || this.elapsed > 62) {
        this.phase = "results";
        this.resultsTime = 0;
      }
    } else {
      this.resultsTime += dt;
      if (this.resultsTime > 7) this.resetMatch();
    }
    this.broadcastSnapshot();
  }

  private resetMatch() {
    this.phase = "countdown";
    this.countdown = 4;
    this.elapsed = 0;
    this.destroyed.clear();
    for (const runner of this.runners.values()) {
      runner.state = createRunner();
      runner.input = { ...EMPTY_INPUT };
    }
  }

  private broadcastSnapshot() {
    this.broadcast("snapshot", {
      phase: this.phase,
      countdown: Math.max(0, this.countdown),
      elapsed: this.elapsed,
      dragonZ: dragonPosition(this.elapsed),
      destroyed: [...this.destroyed],
      finishZ: COURSE.finishZ,
      players: [...this.runners.entries()].map(([id, runner]) => ({
        id,
        name: runner.name,
        position: runner.state.position,
        alive: runner.state.alive,
        finished: runner.state.finished,
      })),
    });
  }
}

const port = Number(process.env.PORT || 2567);
const gameServer = new Server({ transport: new WebSocketTransport() });
gameServer.define("dragon_escape", DragonEscapeRoom);
await gameServer.listen(port);
console.log(`Dragon Escape public match server listening on ws://localhost:${port}`);
