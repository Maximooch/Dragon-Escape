"""Initialize an ignored local runtime from templates and run with Java 8/Docker."""
from pathlib import Path
import os, secrets, shutil, subprocess, json, time
root=Path(__file__).resolve().parent.parent
runtime=root/'runtime'
java=os.environ.get('JAVA8') or shutil.which('java')
if not java: raise SystemExit('Set JAVA8 to a Java 8 executable.')
version=subprocess.run([java,'-version'],capture_output=True,text=True)
if '1.8.' not in version.stdout+version.stderr:raise SystemExit('This legacy server requires Java 8. Set JAVA8 accordingly.')
subprocess.run(['docker','info'],check=True,stdout=subprocess.DEVNULL)
config=root/'.local-db.json'
if not config.exists():config.write_text(json.dumps({'password':secrets.token_hex(24),'root_password':secrets.token_hex(24)}))
cfg=json.loads(config.read_text());name='parcade-de-checkpoint-db'
if subprocess.run(['docker','container','inspect',name],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL).returncode:
 subprocess.run(['docker','run','-d','--name',name,'-p','127.0.0.1:3308:3306','-e','MARIADB_DATABASE=dragonescape','-e','MARIADB_USER=dragonescape','-e','MARIADB_PASSWORD='+cfg['password'],'-e','MARIADB_ROOT_PASSWORD='+cfg['root_password'],'-v',name+':/var/lib/mysql','mariadb:10.11'],check=True)
else:subprocess.run(['docker','start',name],check=True)
for _ in range(60):
 if not subprocess.run(['docker','exec','-e','MYSQL_PWD='+cfg['password'],name,'mariadb','-udragonescape','dragonescape','-e','SELECT 1'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL).returncode:break
 time.sleep(2)
else:raise SystemExit('Database did not become ready. Check Docker logs.')
if not runtime.exists():
 pending=root/'runtime-preparing'
 if pending.exists():raise SystemExit('Incomplete preparation exists; inspect/remove runtime-preparing before retrying.')
 shutil.copytree(root/'server',pending)
 for world in (root/'maps').iterdir():
  if world.is_dir():shutil.copytree(world,pending/world.name)
 sql='mysql:\n  hostname: 127.0.0.1\n  port: 3308\n  username: dragonescape\n  password: '+cfg['password']+'\n  database: dragonescape\n'
 (pending/'plugins/DragonEscape/mysql.yml').write_text(sql)
 (pending/'plugins/Cubics/mysql.config').write_text(sql)
 (pending/'plugins/Link/config.yml').write_text('host: 127.0.0.1\nport: "3308"\nuser: dragonescape\npassword: '+cfg['password']+'\ndatabase: dragonescape\n')
 pending.rename(runtime)
if 'eula=true' not in (runtime/'eula.txt').read_text():raise SystemExit('Read the Minecraft EULA. If you agree, set eula=true in runtime/eula.txt, then rerun.')
raise SystemExit(subprocess.call([java,'-Xms512M','-Xmx3G','-jar','spigot.jar','nogui'],cwd=runtime))
