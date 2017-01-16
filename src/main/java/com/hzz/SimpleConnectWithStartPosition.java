package com.hzz;

import com.google.code.or.OpenReplicator;
import com.google.code.or.binlog.BinlogEventListener;
import com.google.code.or.binlog.BinlogEventV4;
import com.google.code.or.binlog.impl.event.*;
import com.google.code.or.common.glossary.Pair;
import com.google.code.or.common.glossary.Row;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SimpleConnectWithStartPosition {
    static Map<Long, TableMapEvent> tableMeta = new HashMap<Long, TableMapEvent>();

    public static void main(String[] args) throws Exception {
        final OpenReplicator or = new OpenReplicator();
        or.setUser("repl");
        or.setPassword("mysql");
        or.setHost("qserver1");
        or.setPort(3306);
        or.setServerId(6789);
        or.setBinlogFileName("master-bin.000007");
        final long pos = getPos();
        System.out.println("pos: " + pos);
        or.setBinlogPosition(pos);
        or.setBinlogEventListener(new BinlogEventListener() {
            public void onEvents(BinlogEventV4 event) {
                System.out.println(event.getClass());
                if (event instanceof TableMapEvent) {
                    TableMapEvent tme = (TableMapEvent) event;
                    tableMeta.put(tme.getTableId(), tme);
                } else if (event instanceof AbstractRowEvent) {
                    //下面这段代码不生效  5.1.73
//                    BinlogEventV4Header header = event.getHeader();
//                    long position = header.getNextPosition();
//                    System.out.println("next pos: " + position);
//                    if (MySQLConstants.WRITE_ROWS_EVENT_V2 == header.getEventType()) {
//                        System.out.println("insert2");
//                    } else if (MySQLConstants.UPDATE_ROWS_EVENT_V2 == header.getEventType()) {
//                        System.out.println("update2");
//                    } else if (MySQLConstants.DELETE_ROWS_EVENT_V2 == header.getEventType()) {
//                        System.out.println("delete2");
//                    } else {
//                        System.out.println("unrec2");
//                    }
                    if (event instanceof AbstractRowEvent) {
                        if (event instanceof DeleteRowsEvent) {
                            System.out.println("delete");
                        } else if (event instanceof UpdateRowsEvent) {
                            UpdateRowsEvent ure = (UpdateRowsEvent) event;
                            TableMapEvent tme = tableMeta.get(ure.getTableId());
                            String dbName = tme.getDatabaseName().toString();
                            String tbName = tme.getTableName().toString();
                            String uca = ure.getUsedColumnsAfter().toString();
                            String ucb = ure.getUsedColumnsBefore().toString();
                            System.out.println(dbName + "#" + tbName + "#" + uca + "#" + ucb);
                            for (Pair<Row> p : ure.getRows()) {
                                Row rb = p.getBefore();
                                Row ra = p.getAfter();
                                System.out.println(rb.toString());
                                System.out.println(ra.toString());
                            }
                            ure.getUsedColumnsAfter();
                            System.out.println("update");
                        } else if (event instanceof WriteRowsEvent) {
                            System.out.println("write");
                        } else {
                            System.out.println("other");
                        }
                    }
                    System.out.println("event: " + event);
                } else if (event instanceof RotateEvent) {
                    System.out.println("变更binlog");
                } else if (event instanceof QueryEvent) {
                    System.out.println("查询");
                } else {
                    System.out.println("不识别的event");
                }
            }

        });
        or.start();
        System.out.println("press q to stop");
        final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            if (line.equals("q")) {
                or.stop(10, TimeUnit.SECONDS);
                break;
            }
        }
    }

    static long getPos() {
        Connection conn = null;
        try {
            String url = "jdbc:mysql://qserver1:3306";
            String user = "repl";
            String password = "mysql";
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(url, user, password);
            PreparedStatement ps = conn.prepareStatement("show master status");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String file = rs.getString("File");
                long poosition = rs.getLong("Position");
                return poosition;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return 0;
    }
}
