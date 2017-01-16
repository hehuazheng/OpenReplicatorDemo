package com.hzz;

import com.google.code.or.OpenReplicator;
import com.google.code.or.binlog.BinlogEventListener;
import com.google.code.or.binlog.BinlogEventV4;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class SimpleConnect {
    public static void main(String[] args) throws Exception {
        final OpenReplicator or = new OpenReplicator();
        or.setUser("repl");
        or.setPassword("mysql");
        or.setHost("qserver1");
        or.setPort(3306);
        or.setServerId(6789);
        or.setBinlogFileName("master-bin.000001");
        or.setBinlogEventListener(new BinlogEventListener() {
            public void onEvents(BinlogEventV4 event) {
                System.out.println("event: " + event);
            }
        });
        or.start();
        System.out.println("press q to stop");
        final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        for(String line = br.readLine(); line != null; line = br.readLine()) {
            if(line.equals("q")) {
                or.stop(10, TimeUnit.SECONDS);
                break;
            }
        }
    }
}
