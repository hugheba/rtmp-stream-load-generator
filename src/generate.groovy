#!/usr/bin/env groovy
/**
 * @auther Bryan Hughes <hugheba@gmail.com>
 */
@Grab(group="org.codehaus.gpars", module="gpars", version="1.2.1")

import groovy.util.OptionAccessor
import groovyx.gpars.GParsPool

def cli = new CliBuilder(usage: './generate.groovy -[hfrc]')
cli.h(longOpt: 'help', 'usage information', required: false)
cli.f(longOpt: 'file', 'input file to stream', required: false, args: 1)
cli.s(longOpt: 'server', 'server URI in format of rtmp://host/application', required: false, args: 1)
cli.c(longOpt: 'concurrency', 'concurrent number of streams', required: false, args: 1)

OptionAccessor opt = cli.parse(args)
if(opt.h) {
    cli.usage()
    return
}

Integer max = (opt.getProperty('concurrecy')?: 1) as Integer
def streamFile = (opt.getProperty('file'))?: './test.mp4'
def rtmpApp = (opt.getProperty('server'))?: 'rtmp://localhost/live'

Closure getcmd = { file, rtmp ->
    def cmdStr = "/usr/local/bin/ffmpeg -i ${file} -vcodec copy -acodec copy -f flv ${rtmp}/test-${new Random().nextInt()}"
    return cmdStr.tokenize(' ')
}

def cmds = []
[1..10].each {
    cmds << getcmd(streamFile, rtmpApp)
}

GParsPool.withPool {
    cmds.eachParallel {
        println "Running: ${it}"
        Process process = new ProcessBuilder().inheritIO().command(it).start()
        process.waitFor()
        println "Exited with code ${process.exitValue()}"
    }
}

def inheritIO(final InputStream src, final PrintStream dest) {
    new Thread(new Runnable() {
        public void run() {
            Scanner sc = new Scanner(src);
            while (sc.hasNextLine()) {
                dest.println(sc.nextLine());
            }
        }
    }).start();
}