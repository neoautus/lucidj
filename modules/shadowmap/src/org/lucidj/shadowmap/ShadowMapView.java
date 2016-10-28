/*
 * Copyright 2016 NEOautus Ltd. (http://neoautus.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.lucidj.shadowmap;

import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.prng.DigestRandomGenerator;
import org.lucidj.runtime.Kernel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.server.Sizeable;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import org.apache.felix.ipojo.annotations.*;
import org.apache.felix.ipojo.annotations.Component;

@Component
@Instantiate
@Provides (specifications = com.vaadin.navigator.View.class)
public class ShadowMapView extends VerticalLayout implements View
{
    final Logger log = LoggerFactory.getLogger (ShadowMapView.class);

    @Property public String title = "ShadowMap";
    @Property public int weight = 250;
    @Property public Resource icon = FontAwesome.MAP_O;

    private String console_text = "";
    private Label console_out_err = new Label ();

    public ShadowMapView ()
    {
        // Delay UI building
        log.info("Slf4j is working: {}", title);

        console_out_err.setWidth (100, Sizeable.Unit.PERCENTAGE);
        console_out_err.setContentMode (ContentMode.HTML);
        console_out_err.setHeightUndefined ();
    }

    public void println (String text)
    {
        console_text += text + "\n";
        String safe_string = SafeHtmlUtils.htmlEscape (console_text);
        String html = "<div style=\"font: 14px/normal 'Monaco', 'Menlo', 'Ubuntu Mono', 'Consolas', 'source-code-pro', monospace; white-space: pre-wrap;\">" +
                safe_string.replace ("\n", "<br />") + "</div>";
        console_out_err.setValue (html);
    }

    public void println ()
    {
        println ("");
    }

    private void buildView()
    {
        setMargin(true);
        addComponent (console_out_err);
        main ();
    }

    private final DigestRandomGenerator generator = new DigestRandomGenerator(new SHA3Digest (512));
    private BaseEncoding base64 = BaseEncoding.base64();

    public String hash(String plainPassword) {
        return hash(plainPassword, salt(128), 512, 101501);
    }

    public String hash(String plainPassword, byte[] salt) {
        return hash(plainPassword, salt, 512, 101501);
    }

    public String hash(String plainPassword, byte[] salt, int keyLength, int iterations) {
//        checkArgument(!isNullOrEmpty(plainPassword), "password can not be empty or null");
//        checkArgument(keyLength > 0, "the key length must be greater than 0");
//        checkArgument(iterations >= 0, "the number of iterations must be positive");

        PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator ();
        generator.init(PBEParametersGenerator.PKCS5PasswordToBytes(
                plainPassword.toCharArray()),
                salt,
                iterations);

        return (encode(salt) + "|" + encode(((KeyParameter) generator.generateDerivedParameters(keyLength)).getKey()));
    }

    public boolean verify(String plainPassword, String hash) {
//        checkArgument(!isNullOrEmpty(plainPassword));
//        checkArgument(!isNullOrEmpty(hash));
        return hash(plainPassword, decode(extractSalt(hash))).equals(hash);
    }

    private byte[] salt(int count) {
        byte[] salt = new byte[count];
        generator.nextBytes(salt);
        return salt;
    }

    private String encode(byte[] input) {
        return (base64.encode (input));
    }

    private byte[] decode(String input)
    {
        return (base64.decode(input));
    }

    private static String extractSalt(String input) {
        return input.substring(0, input.indexOf("|"));
    }

    private byte[] getSalt() throws NoSuchAlgorithmException
    {
        SecureRandom sr = SecureRandom.getInstance ("SHA1PRNG");

        byte[] salt = new byte[16];
        sr.nextBytes(salt);

        return (salt);
    }

//    public void test_bouncy (byte[] salt)
//    {
//        PBEParametersGenerator generator = new PKCS5S2ParametersGenerator ();
//        generator.init(PBEParametersGenerator.PKCS5PasswordToUTF8Bytes(("password").toCharArray()), salt, 1000);
//        KeyParameter params = (KeyParameter)generator.generateDerivedParameters(128);
//        System.out.println(Arrays.toString(params.getKey()));
//    }

    private String generateStrongPasswordHash (String password) throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        int iterations = 1000;
        char[] chars = password.toCharArray();
        byte[] salt = getSalt();

        PBEKeySpec spec = new PBEKeySpec (chars, salt, iterations, 64 * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] hash = skf.generateSecret(spec).getEncoded();
        return iterations + ":" + toHex(salt) + ":" + toHex(hash);
    }

    private String toHex(byte[] array) throws NoSuchAlgorithmException
    {
        BigInteger bi = new BigInteger (1, array);
        String hex = bi.toString(16);
        int paddingLength = (array.length * 2) - hex.length();
        if(paddingLength > 0)
        {
            return String.format("%0"  +paddingLength + "d", 0) + hex;
        }else{
            return hex;
        }
    }

    public static class Layout extends ShadowMap
    {
        public int x;
        public int y;
        public Integer z, z1;
        public Boolean b, b1;
        public TitleAttributes title;
        public String null_string;
        public String[] things;
    }

    public static class TitleAttributes extends ShadowMap
    {
        public String title;
        public boolean bold;
        public boolean italic;
        public int fontsize;
        public Integer[] numbers;
    }

    private void main ()
    {
        println ("Testando serialização GSON");
        println ("==========================");

        println ();
        println ("Kernel.apiLevel () = " + Kernel.apiLevel ());
        println ();

        // Configure GSON
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeHierarchyAdapter(ShadowMap.class, new ShadowMapSerializer ());
        gsonBuilder.setPrettyPrinting();
        gsonBuilder.serializeNulls ();
        final Gson gson = gsonBuilder.create();

        try
        {
            String  originalPassword = "password";
            String generatedSecuredPasswordHash = generateStrongPasswordHash (originalPassword);

            println(generatedSecuredPasswordHash);
        }
        catch (Exception ignore) {};

        println ("Hash: " + hash ("helloworld"));

        final Layout l = new Layout ();
        l.x = 10;
        l.y = 99;
        l.b1 = true;
        l.z1 = 199;
        l.things = new String[] { "Car", "Fridge", "Pizza", "Coke" };
        l.title = new TitleAttributes ();
        l.title.title = "The Title";
        l.title.bold = true;
        l.title.fontsize = 16;
        l.set ("Shadowy", "The Ghost Field!");
        l.set ("Shadows2", 314159265);
        l.title.set ("fontstyle", "TimesNewRoman");
        l.title.numbers = new Integer[] { 1, 3, 5, 7, 11 };

        println (gson.toJson(l));

        l.set ("title.italic", true);
        println ("---");
        println (gson.toJson(l));

        l.set ("title.customfield", "Very custom!");
        println ("---");
        println (gson.toJson(l));

        l.set ("y", 77);
        println ("---");
        println (gson.toJson(l));

        l.set ("title.dontexist.x", "Xis");
        l.set ("title.dontexist.y", 88);
        l.set ("title.dontexist.units", "PICSEIS");
        l.set ("title.fontstyles", new String[] { "Times", "Helvetica", "Arial" });
        println ("---");
        String json = gson.toJson(l);
        println (json);

        println ("Testando deserialização GSON");
        println ("============================");
        println ();

        Layout l2 = gson.fromJson (json, Layout.class);

        println (gson.toJson(l2));
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event)
    {
        // TODO Auto-generated method stub
        if (getComponentCount() == 0)
        {
            buildView();
        }
    }
}

// EOF
