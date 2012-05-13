/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.openspaces.admin.pu.config;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlRootElement;

import org.springframework.beans.factory.annotation.Required;

import com.gigaspaces.security.Authority;
import com.gigaspaces.security.directory.User;

/**
 * @author itaif
 * @since 9.0.1
 */
@XmlRootElement(name="user-details")
public class UserDetailsConfig {

    private Authority[] authorities = new Authority[0];
    private String password;
    private String username;

    public Authority[] getAuthorities() {
        return authorities;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public void setAuthorities(Authority[] authorities) {
        this.authorities = authorities;
    }

    @Required
    public void setPassword(String password) {
        this.password = password;
    }

    @Required
    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(authorities);
        result = prime * result + ((password == null) ? 0 : password.hashCode());
        result = prime * result + ((username == null) ? 0 : username.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        UserDetailsConfig other = (UserDetailsConfig) obj;
        if (!Arrays.equals(authorities, other.authorities))
            return false;
        if (password == null) {
            if (other.password != null)
                return false;
        } else if (!password.equals(other.password))
            return false;
        if (username == null) {
            if (other.username != null)
                return false;
        } else if (!username.equals(other.username))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "UserDetailsConfig ["
                + (authorities != null ? "authorities=" + Arrays.toString(authorities) + ", " : "")
                + (password != null ? "password=" + "***, " : "")
                + (username != null ? "username=" + username : "") + "]";
    }
    
    public User toUser() {
        return new User(username,password,authorities);
    }
}
