"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { DragonEscapeRuntime, type GameView } from "../lib/client/DragonEscapeRuntime";

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

  useEffect(() => {
    if (!canvasRef.current) return;
    const runtime = new DragonEscapeRuntime(canvasRef.current, setView);
    runtimeRef.current = runtime;
    runtime.mount();
    return () => runtime.destroy();
  }, []);

  const join = useCallback(() => {
    runtimeRef.current?.join(name.trim() || "Runner");
  }, [name]);

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

  return (
    <main className="game-shell">
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
          <p className="eyebrow">PUBLIC PLAYTEST // BUILD 004</p>
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
          <div className="control-grid">
            <span><kbd>WASD</kbd> MOVE</span>
            <span><kbd>SPACE</kbd> JUMP</span>
            <span><kbd>SHIFT</kbd> SPRINT</span>
            <span><kbd>Q</kbd> LEAP</span>
          </div>
          <p className="tip">Click the course to capture your mouse. Press Esc to release it.</p>
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

      {view.phase !== "menu" && !view.pointerLocked && view.phase !== "results" && (
        <button className="look-prompt" onClick={focusGame}>
          <b>CLICK TO LOOK</b>
          <span>Mouse controls camera · Esc releases</span>
        </button>
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

      <footer className="footer-note">FIRST-PERSON PARKOUR · ORIGINAL MVP · DESKTOP BROWSERS</footer>
    </main>
  );
}
