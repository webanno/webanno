/*
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.security.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
/**
 * The persistence object for authority the user have. Authorities can be either {@code ROLE_ADMIN}
 * or {@code ROLE_USER}
 *
 */
@Entity
@Table(name = "authorities", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "authority", "username" }) })
public class Authority
    implements Serializable
{
    private static final long serialVersionUID = -1490540239189868920L;

    @Id
    @GeneratedValue
    private long id;

    @Column(nullable = false)
    @JoinColumn(name = "authority")
    private String authority;

    @ManyToOne
    @JoinColumn(name = "username")
    private User username;

    public long getId()
    {
        return id;
    }

    public void setId(long aId)
    {
        id = aId;
    }

    public String getAuthority()
    {
        return authority;
    }

    public void setAuthority(String authority)
    {
        this.authority = authority;
    }

    public User getUsername()
    {
        return username;
    }

    public void setUsername(User username)
    {
        this.username = username;
    }


}
