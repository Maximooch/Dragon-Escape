"use client";

import { useCallback, useEffect, useRef, useState, type PointerEvent as ReactPointerEvent } from "react";
import { DragonEscapeRuntime, type GameView } from "../lib/client/DragonEscapeRuntime";
import { joystickAxes, type TouchAxes } from "../lib/client/touch";

const initialView: GameView = {
  phase: "menu",
  banner: "",
  time: 0,
  progress: 0,
  place: 1,
  playerCount: 6,
  checkpoint: "The Gate",
  leapCooldown: 0,
  online: false,
  eliminated: false,
  pointerLocked: false,
  audioState: "idle",
};

export function GameClient() {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const runtimeRef = useRef<DragonEscapeRuntime | null>(null);
  const [view, setView] = useState(initialView);
  const [name, setName] = useState("Runner");
  const [muted, setMuted] = useState(false);
  const [touchMode, setTouchMode] = useState(false);
  const [stick, setStick] = useState<TouchAxes>({ strafe: 0, forward: 0 });
  const [sprintActive, setSprintActive] = useState(false);
  const movePadRef = useRef<HTMLDivElement>(null);
  const movePointer = useRef<number | null>(null);
  const lookPointer = useRef<number | null>(null);
  const lookLast = useRef({ x: 0, y: 0 });

  useEffect(() => {
    const coarse = window.matchMedia("(pointer: coarse)");
    const detect = () => setTouchMode(coarse.matches || navigator.maxTouchPoints > 0);
    const detectTouch = (event: PointerEvent) => {
      if (event.pointerType === "touch") setTouchMode(true);
    };
    detect();
    coarse.addEventListener("change", detect);
    window.addEventListener("pointerdown", detectTouch, { passive: true });
    return () => {
      coarse.removeEventListener("change", detect);
      window.removeEventListener("pointerdown", detectTouch);
    };
  }, []);

  useEffect(() => {
    if (!canvasRef.current) return;
    const runtime = new DragonEscapeRuntime(canvasRef.current, setView);
    runtimeRef.current = runtime;
    runtime.mount();
    return () => runtime.destroy();
  }, []);

  useEffect(() => {
    runtimeRef.current?.setTouchMode(touchMode);
  }, [touchMode]);

  const join = useCallback(() => {
    if (touchMode) (document.activeElement as HTMLElement | null)?.blur();
    setStick({ strafe: 0, forward: 0 });
    runtimeRef.current?.setTouchMovement({ strafe: 0, forward: 0 });
    runtimeRef.current?.join(name.trim() || "Runner");
  }, [name, touchMode]);

  const focusGame = () => runtimeRef.current?.capturePointer();
  const testSound = () => {
    if (muted) {
      setMuted(false);
      runtimeRef.current?.setMuted(false);
    } else {
      runtimeRef.current?.testSound();
    }
  };
  const toggleSound = () => {
    if (!muted && view.audioState !== "ready") {
      runtimeRef.current?.testSound();
      return;
    }
    const next = !muted;
    setMuted(next);
    runtimeRef.current?.setMuted(next);
  };

  const audioLabel = muted
    ? "SOUND OFF"
    : view.audioState === "ready"
      ? "AUDIO READY"
      : view.audioState === "blocked"
        ? "AUDIO BLOCKED"
        : "SOUND ON";

  const updateMove = (event: ReactPointerEvent<HTMLDivElement>) => {
    const bounds = movePadRef.current?.getBoundingClientRect();
    if (!bounds) return;
    const axes = joystickAxes(event.clientX, event.clientY, bounds);
    setStick(axes);
    runtimeRef.current?.setTouchMovement(axes);
  };

  const startMove = (event: ReactPointerEvent<HTMLDivElement>) => {
    event.preventDefault();
    movePointer.current = event.pointerId;
    event.currentTarget.setPointerCapture(event.pointerId);
    updateMove(event);
  };

  const moveMove = (event: ReactPointerEvent<HTMLDivElement>) => {
    if (movePointer.current !== event.pointerId) return;
    event.preventDefault();
    updateMove(event);
  };

  const endMove = (event: ReactPointerEvent<HTMLDivElement>) => {
    if (movePointer.current !== event.pointerId) return;
    movePointer.current = null;
    setStick({ strafe: 0, forward: 0 });
    runtimeRef.current?.setTouchMovement({ strafe: 0, forward: 0 });
  };

  const startLook = (event: ReactPointerEvent<HTMLDivElement>) => {
    event.preventDefault();
    lookPointer.current = event.pointerId;
    lookLast.current = { x: event.clientX, y: event.clientY };
    event.currentTarget.setPointerCapture(event.pointerId);
  };

  const moveLook = (event: ReactPointerEvent<HTMLDivElement>) => {
    if (lookPointer.current !== event.pointerId) return;
    event.preventDefault();
    runtimeRef.current?.touchLook(event.clientX - lookLast.current.x, event.clientY - lookLast.current.y);
    lookLast.current = { x: event.clientX, y: event.clientY };
  };

  const endLook = (event: ReactPointerEvent<HTMLDivElement>) => {
    if (lookPointer.current === event.pointerId) lookPointer.current = null;
  };

  const setJump = (active: boolean, event: ReactPointerEvent<HTMLButtonElement>) => {
    event.preventDefault();
    if (active) event.currentTarget.setPointerCapture(event.pointerId);
    runtimeRef.current?.setTouchAction("jump", active);
  };

  const toggleSprint = () => {
    const next = !sprintActive;
    setSprintActive(next);
    runtimeRef.current?.setTouchAction("sprint", next);
  };

  return (
    <main className={`game-shell ${touchMode ? "touch-mode" : ""}`}>
      <canvas ref={canvasRef} className="game-canvas" aria-label="Dragon Escape game world" onClick={focusGame} />
      <div className="grain" aria-hidden="true" />

      <header className="topbar">
        <div className="wordmark"><span>DRAGON</span> ESCAPE</div>
        <div className={`connection ${view.online ? "online" : ""}`}>
          <i /> {view.online ? "PUBLIC ROOM" : "DEMO RIVALS"}
        </div>
        <button className="sound-button" onClick={toggleSound} aria-label={muted ? "Unmute" : view.audioState === "ready" ? "Mute" : "Enable sound"}>
          {audioLabel}
        </button>
      </header>

      {view.phase === "menu" && (
        <section className="menu-panel">
          <p className="eyebrow">PUBLIC PLAYTEST // BUILD 005</p>
          <h1>OUTRUN<br /><em>THE END.</em></h1>
          <p className="intro">Salto is collapsing. Climb the floating archipelago before the dragon tears it from the sky.</p>
          <label className="name-field">
            <span>RUNNER NAME</span>
            <input value={name} maxLength={18} onChange={(event) => setName(event.target.value)} onKeyDown={(event) => event.key === "Enter" && join()} />
          </label>
          <button className="join-button" onClick={join}><span>JOIN NEXT RACE</span><b>→</b></button>
          <button className={`audio-test ${view.audioState}`} onClick={testSound}>
            <span>TEST SOUND</span>
            <b>{view.audioState === "ready" ? "AUDIBLE?" : view.audioState.toUpperCase()}</b>
          </button>
          {touchMode ? (
            <div className="control-grid touch-legend">
              <span><kbd>LEFT</kbd> MOVE</span>
              <span><kbd>DRAG</kbd> LOOK</span>
              <span><kbd>JUMP</kbd> CLIMB</span>
              <span><kbd>LEAP</kbd> ESCAPE</span>
            </div>
          ) : (
            <div className="control-grid">
              <span><kbd>WASD</kbd> MOVE</span>
              <span><kbd>SPACE</kbd> JUMP</span>
              <span><kbd>SHIFT</kbd> SPRINT</span>
              <span><kbd>Q</kbd> LEAP</span>
            </div>
          )}
          <p className="tip">{touchMode ? "Landscape recommended · headphones optional" : "Click the course to capture your mouse. Press Esc to release it."}</p>
        </section>
      )}

      {view.phase !== "menu" && (
        <>
          <div className="crosshair" aria-hidden="true"><i /><i /></div>
          <aside className="race-card">
            <p>SALTO // OWNED TEST MAP</p>
            <strong>{view.checkpoint}</strong>
            <div className="progress-track"><i style={{ width: `${view.progress}%` }} /></div>
            <div className="race-stats">
              <span><b>{view.place}</b><small>PLACE</small></span>
              <span><b>{view.time.toFixed(1)}</b><small>SECONDS</small></span>
              <span><b>{view.progress}%</b><small>COURSE</small></span>
            </div>
          </aside>
          <aside className={`ability ${view.leapCooldown <= 0 ? "ready" : ""}`}>
            <span>Q</span>
            <div><b>DRAGON LEAP</b><small>{view.leapCooldown <= 0 ? "READY" : `${view.leapCooldown.toFixed(1)}s`}</small></div>
          </aside>
        </>
      )}

      {view.phase !== "menu" && !view.pointerLocked && !touchMode && view.phase !== "results" && (
        <button className="look-prompt" onClick={focusGame}>
          <b>CLICK TO LOOK</b>
          <span>Mouse controls camera · Esc releases</span>
        </button>
      )}

      {touchMode && view.phase !== "menu" && view.phase !== "results" && (
        <section className="touch-controls" aria-label="Mobile game controls">
          <div
            className="touch-look-zone"
            role="presentation"
            onPointerDown={startLook}
            onPointerMove={moveLook}
            onPointerUp={endLook}
            onPointerCancel={endLook}
            onLostPointerCapture={endLook}
          >
            <span>SWIPE TO LOOK</span>
          </div>
          <div
            ref={movePadRef}
            className="touch-move-pad"
            role="group"
            aria-label="Movement joystick"
            onPointerDown={startMove}
            onPointerMove={moveMove}
            onPointerUp={endMove}
            onPointerCancel={endMove}
            onLostPointerCapture={endMove}
          >
            <i style={{ transform: `translate(calc(-50% + ${stick.strafe * 30}px), calc(-50% + ${-stick.forward * 30}px))` }} />
          </div>
          <div className="touch-actions">
            <button
              className={`touch-action leap ${view.leapCooldown <= 0 ? "ready" : ""}`}
              disabled={view.leapCooldown > 0}
              onPointerDown={(event) => {
                event.preventDefault();
                runtimeRef.current?.triggerTouchLeap();
              }}
            >
              <b>LEAP</b><small>{view.leapCooldown <= 0 ? "READY" : view.leapCooldown.toFixed(1)}</small>
            </button>
            <button
              className={`touch-action sprint ${sprintActive ? "active" : ""}`}
              aria-pressed={sprintActive}
              onClick={toggleSprint}
            >
              <b>SPRINT</b><small>{sprintActive ? "ON" : "TOGGLE"}</small>
            </button>
            <button
              className="touch-action jump"
              onPointerDown={(event) => setJump(true, event)}
              onPointerUp={(event) => setJump(false, event)}
              onPointerCancel={(event) => setJump(false, event)}
              onLostPointerCapture={(event) => setJump(false, event)}
            >
              <b>JUMP</b><small>HOLD</small>
            </button>
          </div>
          <p className="rotate-hint">ROTATE FOR A WIDER VIEW</p>
        </section>
      )}

      {view.banner && <div className={`center-banner ${view.eliminated ? "danger" : ""}`}>{view.banner}</div>}

      {view.phase === "results" && (
        <section className="results-panel">
          <p className="eyebrow">RACE COMPLETE</p>
          <h2>{view.eliminated ? "CONSUMED" : "SANCTUARY"}</h2>
          <p>{view.eliminated ? `The dragon caught you at ${view.progress}% of the course.` : `You escaped in ${view.time.toFixed(2)} seconds.`}</p>
          <div className="result-line"><span>FINAL PLACE</span><b>#{view.place} / {view.playerCount}</b></div>
          <button className="join-button" onClick={join}><span>RACE AGAIN</span><b>↻</b></button>
        </section>
      )}

      <footer className="footer-note">FIRST-PERSON PARKOUR · ORIGINAL MVP · DESKTOP + MOBILE</footer>
    </main>
  );
}
