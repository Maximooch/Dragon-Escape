"""Rebuild maintained patches with JDK 9+ targeting Java 8. No runtime is modified."""
from pathlib import Path
import os, shutil, subprocess, zipfile
root=Path(__file__).resolve().parent.parent
build=root/'build'; build.mkdir(exist_ok=True)
javac=str(Path(os.environ['JAVA_HOME'])/'bin/javac.exe') if os.environ.get('JAVA_HOME') else shutil.which('javac')
if not javac: raise SystemExit('Set JAVA_HOME to JDK 9+ or put javac on PATH.')
plugins=root/'server/plugins'
cp=os.pathsep.join(map(str,[root/'server/spigot.jar',plugins/'FastAsyncWorldEdit.jar',plugins/'WorldEdit.jar',plugins/'*']))
prefix='me/radoje17/dragonescape/'
names=['Arena','DragonEscape','Game','GameManager','dragon/Dragon','geometry/Sphere','utils/ArenaUtils']
sources=[root/'decompiled_output'/(prefix+n+'.java') for n in names]
sources += [root/'tools/LocalPlayers.java',root/'patches/parcade/me/tim/parcade/other/XpLeaderboard.java']
subprocess.run([javac,'--release','8','-cp',cp,'-d',str(build)]+list(map(str,sources)),check=True)
def patch(jar,prefixes):
 temporary=jar.with_suffix('.tmp')
 with zipfile.ZipFile(jar) as old,zipfile.ZipFile(temporary,'w',zipfile.ZIP_DEFLATED) as new:
  for item in old.infolist():
   if any(item.filename==p+'.class' or item.filename.startswith(p+'$') for p in prefixes):continue
   new.writestr(item,old.read(item.filename))
  for p in prefixes:
   for f in (build/Path(p).parent).glob(Path(p).name+'*.class'):new.write(f,f.relative_to(build).as_posix())
 temporary.replace(jar)
patch(plugins/'Dragon Escape.jar',[prefix+n for n in names])
patch(plugins/'Parcade.jar',['me/tim/parcade/other/XpLeaderboard'])
with zipfile.ZipFile(plugins/'LocalPlayers.jar','w',zipfile.ZIP_DEFLATED) as z:
 z.write(build/'LocalPlayers.class','LocalPlayers.class');z.write(root/'tools/plugin.yml','plugin.yml')
shutil.copy2(plugins/'Dragon Escape.jar',root/'DragonEscape.jar')
print('Rebuilt DragonEscape, Parcade XP patch, and LocalPlayers; runtime untouched.')
