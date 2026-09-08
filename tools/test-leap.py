"""Compile and run deterministic checks for the Mineplex leap reference port."""
from pathlib import Path
import os
import shutil
import subprocess

root = Path(__file__).resolve().parent.parent
build = root / "build" / "leap-test"
javac = str(Path(os.environ["JAVA_HOME"]) / "bin" / "javac.exe") if os.environ.get("JAVA_HOME") else shutil.which("javac")
java = str(Path(os.environ["JAVA_HOME"]) / "bin" / "java.exe") if os.environ.get("JAVA_HOME") else shutil.which("java")

if not javac or not java:
    raise SystemExit("Set JAVA_HOME to JDK 9+ or put java/javac on PATH.")

if build.exists():
    shutil.rmtree(build)
build.mkdir(parents=True)

subprocess.run(
    [
        javac,
        "--release",
        "8",
        "-cp",
        str(root / "server" / "spigot.jar"),
        "-d",
        str(build),
        str(root / "decompiled_output" / "me" / "radoje17" / "dragonescape" / "kits" / "MineplexLeapPhysics.java"),
        str(root / "tools" / "MineplexLeapPhysicsTest.java"),
    ],
    check=True,
)

subprocess.run(
    [
        java,
        "-cp",
        os.pathsep.join([str(build), str(root / "server" / "spigot.jar")]),
        "me.radoje17.dragonescape.kits.MineplexLeapPhysicsTest",
    ],
    check=True,
)
