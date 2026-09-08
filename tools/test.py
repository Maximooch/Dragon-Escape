"""Run real-server integration checks on port 25566 with an isolated SQL schema/world."""
from pathlib import Path
import os,json,shutil,subprocess,zipfile
root=Path(__file__).resolve().parent.parent
sandbox=root/'test-runtime';cfg=json.loads((root/'.local-db.json').read_text())
java=os.environ.get('JAVA8') or shutil.which('java')
javac=str(Path(os.environ['JAVA_HOME'])/'bin/javac.exe') if os.environ.get('JAVA_HOME') else shutil.which('javac')
if not java or not javac:raise SystemExit('Configure JAVA8 and JAVA_HOME.')
if not (root/'runtime/eula.txt').exists() or 'eula=true' not in (root/'runtime/eula.txt').read_text():raise SystemExit('Accept EULA in runtime first, if you agree.')
if sandbox.exists():shutil.rmtree(sandbox)
shutil.copytree(root/'server',sandbox)
(sandbox/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=25566\nonline-mode=true\nlevel-name=DE\nlevel-type=FLAT\ngenerator-settings=2;0;1;\nallow-nether=false\nview-distance=3\n')
(sandbox/'eula.txt').write_text('eula=true\n')
keep=['FastAsyncWorldEdit.jar','WorldEdit.jar','Dragon Escape.jar','Cubics.jar','LocalPlayers.jar']
for f in (sandbox/'plugins').glob('*.jar'):
 if f.name not in keep:f.unlink()
subprocess.run(['docker','exec','-e','MYSQL_PWD='+cfg['root_password'],'parcade-de-checkpoint-db','mariadb','-uroot','-e',"CREATE DATABASE IF NOT EXISTS dragonescape_test; GRANT ALL ON dragonescape_test.* TO 'dragonescape'@'%';"],check=True)
for name in ['DragonEscape/mysql.yml','Cubics/mysql.config']:
 f=sandbox/'plugins'/name;f.write_text(f.read_text().replace('CHANGE_ME',cfg['password']).replace('3307','3308').replace('database: dragonescape','database: dragonescape_test'))
build=root/'build';build.mkdir(exist_ok=True)
cp=os.pathsep.join(map(str,[root/'server/spigot.jar',root/'server/plugins/FastAsyncWorldEdit.jar',root/'server/plugins/WorldEdit.jar',root/'server/plugins/*']))
subprocess.run([javac,'--release','8','-cp',cp,'-d',str(build),str(root/'tools/PerformanceChecks.java')],check=True)
with zipfile.ZipFile(sandbox/'plugins/PerformanceChecks.jar','w') as z:
 z.write(build/'PerformanceChecks.class','PerformanceChecks.class');z.writestr('plugin.yml','name: PerformanceChecks\nversion: 1.0\nmain: PerformanceChecks\ndepend: [DragonEscape]\n')
with (sandbox/'test.log').open('w') as log:
 result=subprocess.run([java,'-Xms256M','-Xmx2G','-jar','spigot.jar','nogui'],cwd=sandbox,stdout=log,stderr=subprocess.STDOUT)
text=(sandbox/'test.log').read_text(errors='replace')
print('\n'.join(l for l in text.splitlines() if 'PASS' in l or 'FAILED' in l))
if result.returncode or 'PERFORMANCE TESTS PASSED: 20' not in text:raise SystemExit('Checks failed: inspect test-runtime/test.log')
