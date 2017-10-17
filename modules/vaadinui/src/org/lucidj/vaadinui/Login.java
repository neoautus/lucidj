/*
 * Copyright 2017 NEOautus Ltd. (http://neoautus.com)
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

package org.lucidj.vaadinui;

import com.vaadin.event.FieldEvents;
import com.vaadin.event.ShortcutAction;
import com.vaadin.event.ShortcutListener;
import com.vaadin.server.Responsive;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.LoginForm;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import org.lucidj.api.SecurityEngine;
import org.lucidj.api.SecuritySubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

public class Login extends LoginForm implements LoginForm.LoginListener
{
    private final static Logger log = LoggerFactory.getLogger (Login.class);
    private static String login_token;

    private TextField userNameField;
    private boolean userNameField_filling;
    private PasswordField passwordField;
    private Button loginButton;

    private Label message_label = new Label ();

    private SecurityEngine security;
    private LoginListener login_listener;

    Login (SecurityEngine security, LoginListener login_listener)
    {
        this.security = security;
        this.login_listener = login_listener;
        addLoginListener (this);
    }

    @Override // LoginForm
    protected Component createContent (TextField userNameField, PasswordField passwordField, Button loginButton)
    {
        // Save the predefined components
        this.userNameField = userNameField;
        this.passwordField = passwordField;
        this.loginButton = loginButton;

        // Make LoginForm container full-screen
        setSizeFull ();

        VerticalLayout layout = new VerticalLayout ();
        layout.setSizeFull ();
        layout.addStyleName ("login-wallpaper");

        final VerticalLayout loginPanel = new VerticalLayout ();
        loginPanel.setSizeUndefined ();
        loginPanel.setMargin (true);
        loginPanel.setSpacing (true);
        Responsive.makeResponsive (loginPanel);
        loginPanel.addStyleName ("card");

        //--------
        // HEADER
        //--------

        final HorizontalLayout labels = new HorizontalLayout ();
        labels.setWidth("100%");

        final Label title = new Label ("<h3><strong>LucidJ</strong> Console</h3>", ContentMode.HTML);
        labels.addComponent (title);
        labels.setExpandRatio (title, 1);

        loginPanel.addComponent(labels);

        //--------
        // FIELDS
        //--------

        HorizontalLayout fields = new HorizontalLayout ();
        fields.setSpacing (true);
        fields.addStyleName ("fields");

        userNameField.setImmediate (true);
        userNameField.setTextChangeEventMode (AbstractTextField.TextChangeEventMode.EAGER);
        final ShortcutListener username_enter_listener = new ShortcutListener ("Next field (Tab)",
            ShortcutAction.KeyCode.ENTER, null)
        {
            @Override
            public void handleAction (Object o, Object o1)
            {
                passwordField.setValue ("");
                passwordField.focus ();
            }
        };
        userNameField.addTextChangeListener (new FieldEvents.TextChangeListener ()
        {
            @Override
            public void textChange (FieldEvents.TextChangeEvent textChangeEvent)
            {
                show_default_message ();

                int new_username_length = textChangeEvent.getText ().length ();

                // Check for autofill
                if (userNameField.isEmpty ()
                    && new_username_length > 1
                    && !userNameField_filling)
                {
                    // This is autofill
                    userNameField.removeShortcutListener (username_enter_listener);
                    userNameField.setCursorPosition (new_username_length);
                    userNameField.setSelectionRange (0, new_username_length);
                }
                else
                {
                    userNameField_filling = true;
                    passwordField.setValue ("");
                    userNameField.addShortcutListener (username_enter_listener);
                }
            }
        });
        userNameField.addFocusListener (new FieldEvents.FocusListener ()
        {
            @Override
            public void focus (FieldEvents.FocusEvent focusEvent)
            {
                // Cursor on username, Enter jump to password
                loginButton.removeClickShortcut ();
                userNameField.addShortcutListener (username_enter_listener);
            }
        });
        userNameField.addBlurListener (new FieldEvents.BlurListener ()
        {
            @Override
            public void blur (FieldEvents.BlurEvent blurEvent)
            {
                // Cursor on password or elsewhere, enter submits
                userNameField.removeShortcutListener (username_enter_listener);
                loginButton.setClickShortcut (ShortcutAction.KeyCode.ENTER);
            }
        });

        passwordField.setImmediate (true);
        passwordField.setTextChangeEventMode (AbstractTextField.TextChangeEventMode.EAGER);
        passwordField.addTextChangeListener (new FieldEvents.TextChangeListener ()
        {
            @Override
            public void textChange (FieldEvents.TextChangeEvent textChangeEvent)
            {
                show_default_message ();
            }
        });

        loginButton.addStyleName (ValoTheme.BUTTON_PRIMARY);
        loginButton.setDisableOnClick (true);

        fields.addComponents (userNameField, passwordField, loginButton);
        fields.setComponentAlignment (loginButton, Alignment.BOTTOM_LEFT);

        loginPanel.addComponent (fields);

        //--------
        // FOOTER
        //--------

        loginPanel.addComponent (new CheckBox ("Remember me", true));

        loginPanel.addComponent (message_label);
        show_default_message ();

        layout.addComponent (loginPanel);
        layout.setComponentAlignment (loginPanel, Alignment.MIDDLE_CENTER);
        return (layout);
    }

    private void show_default_message ()
    {
        message_label.setCaptionAsHtml (true);
        message_label.setCaption ("Please enter your username and password to access the system.");
    }

    @Override // LoginForm.LoginListener
    public void onLogin (LoginEvent loginEvent)
    {
        if (passwordField.isEmpty ())
        {
            loginButton.setEnabled (true);
            return;
        }

        SecuritySubject current_user = security.getSubject();
        String username = loginEvent.getLoginParameter("username");
        String password = loginEvent.getLoginParameter("password");

        try
        {
            // TODO: http://stackoverflow.com/questions/14516851/shiro-complaining-there-is-no-session-with-id-xxx-with-defaultsecuritymanager
            current_user.login (username, password);
        }
        catch (Exception ignore) {};

        if (current_user.isAuthenticated ())
        {
            login_listener.loginSuccessful ();
        }
        else
        {
            // Probably the password is wrong
            passwordField.setValue ("");
            passwordField.setEnabled(true);
            passwordField.focus ();
            loginButton.setEnabled (true);

            // Show invalid pass message
            message_label.setCaption ("<font color='red'><b>Invalid username or password, please try again.</b></font>");
        }
    }

    //=========================================================================================
    // LOGIN TOKEN SUPPORT
    //=========================================================================================

    static void configureLoginToken ()
    {
        Path token_file = Paths.get (System.getProperty ("system.home"), "cache/login-token.txt");

        if (System.getProperty ("user.conf") == null)
        {
            // On server mode the login token is disabled
            try
            {
                Files.delete (token_file);
            }
            catch (NoSuchFileException ignore) {}
            catch (IOException e)
            {
                log.warn ("Exception removing: {}", token_file, e);
            }
            return;
        }

        try
        {
            // Read the login token from existing file
            login_token = new String (Files.readAllBytes (token_file), StandardCharsets.UTF_8);
        }
        catch (IOException ignore) {};

        if (login_token == null)
        {
            // Create the random-ish string
            login_token = UUID.randomUUID ().toString ().replace ("-", "").toLowerCase (Locale.ENGLISH);

            try
            {
                Files.write (token_file, login_token.getBytes ());
            }
            catch (IOException e)
            {
                log.warn ("Exception creating: {}", token_file, e);
            }
        }
    }

    static boolean isValidLoginToken (String provided_login_token)
    {
        if (login_token == null)
        {
            // We may have the login token stored
            configureLoginToken ();
        }
        return (login_token != null && login_token.equals (provided_login_token));
    }

    //=========================================================================================
    // INTERFACES
    //=========================================================================================

    public interface LoginListener extends Serializable
    {
        void loginSuccessful ();
    }
}

// EOF
