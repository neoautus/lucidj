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

package org.rationalq.security;

import com.vaadin.annotations.StyleSheet;
import com.vaadin.event.ShortcutAction;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.*;
import com.vaadin.shared.Position;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import xyz.kuori.shiro.Shiro;
import org.apache.felix.ipojo.annotations.*;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;

@Component
@Instantiate
@Provides (specifications = com.vaadin.navigator.View.class)
@StyleSheet("vaadin://security/wallpaper.css")
public class Login extends VerticalLayout implements View
{
    @Property public String title = "Login";
    @Property public int weight = 950;
    @Property public Resource icon = FontAwesome.LOCK;
    @Property public boolean visible = false;
    @Property private String navid = "login";

    @Requires private Shiro shiro;

    private TextField username;
    private PasswordField password;
    private Button login;
    private Button forgotPassword;

    private void buildView()
    {
        setSizeFull();
        addStyleName("login-wallpaper");

        final VerticalLayout loginPanel = new VerticalLayout();
        loginPanel.setSizeUndefined();
        loginPanel.setMargin(true);
        loginPanel.setSpacing(true);
        Responsive.makeResponsive(loginPanel);
        loginPanel.addStyleName("card");

        // LABELS
        final HorizontalLayout labels = new HorizontalLayout();
        labels.setWidth("100%");

        final Label title = new Label(
                "<h3><strong>LucidJ</strong> Console</h3>", ContentMode.HTML);
        title.setSizeUndefined();
        labels.addComponent(title);
        labels.setExpandRatio(title, 1);

        loginPanel.addComponent(labels);

        // FIELDS

        HorizontalLayout fields = new HorizontalLayout();
        fields.setSpacing(true);
        fields.addStyleName("fields");

        username = new TextField("Username");
        username.setIcon(FontAwesome.USER);
        username.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);

        password = new PasswordField("Password");
        password.setIcon(FontAwesome.LOCK);
        password.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);

        final Button signin = new Button("Sign In");
        signin.addStyleName(ValoTheme.BUTTON_PRIMARY);
        signin.setClickShortcut(ShortcutAction.KeyCode.ENTER);
        //signin.focus();
        signin.setDisableOnClick(true);
        signin.addClickListener(new Button.ClickListener()
        {
            @Override
            public void buttonClick(Button.ClickEvent event)
            {
                Subject current_user = shiro.getSubject();

                UsernamePasswordToken token = new UsernamePasswordToken
                (
                    username.getValue(), password.getValue()
                );

                try
                {
                    // TODO: http://stackoverflow.com/questions/14516851/shiro-complaining-there-is-no-session-with-id-xxx-with-defaultsecuritymanager
                    current_user.login(token);
                    getUI().getNavigator().navigateTo("");
                }
                catch (Exception oops)
                {
                    password.setValue ("");
                    signin.setEnabled(true);
                    // Show invalid pass message
                    Notification.show("Invalid password!");
                }
            }
        });

        fields.addComponents(username, password, signin);
        fields.setComponentAlignment(signin, Alignment.BOTTOM_LEFT);

        loginPanel.addComponent(fields);

        loginPanel.addComponent(new CheckBox("Remember me", true));

//        loginPanel.addComponent(forgotPassword = new Button("Forgot password?"));
//        forgotPassword.addClickListener(new Button.ClickListener() {
//            @Override
//            public void buttonClick(Button.ClickEvent event) {
//                showNotification(new Notification("Hint: Try anything"));
//            }
//        });
//        forgotPassword.addStyleName(ValoTheme.BUTTON_LINK);

        addComponent(loginPanel);
        setComponentAlignment(loginPanel, Alignment.MIDDLE_CENTER);

//        Notification notification = new Notification
//        (
//            "Welcome to Dashboard Demo",
//            "<span>This application is not real, it only demonstrates an application built with the <a href=\"https://vaadin.com\">Vaadin framework</a>.</span> <span>No username or password is required, just click the <b>Sign In</b> button to continue.</span>"
//        );

        //showNotification(notification, 5);
    }

    private void showNotification(Notification notification, int secs)
    {
        notification.setHtmlContentAllowed(true);
        notification.setStyleName("tray dark small closable login-help");
        notification.setPosition(Position.BOTTOM_CENTER);
        notification.setDelayMsec(secs * 1000);
        notification.show(Page.getCurrent());
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event)
    {
        if (getComponentCount() == 0)
        {
            buildView();
        }

        username.focus();
    }
}

// EOF
