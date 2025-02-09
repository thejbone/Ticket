/*
 * Copyright 2018 Alex Thomson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.lxgaming.ticket.api.data;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

public class TicketData implements Comparable<TicketData> {
    
    private int id;
    private UUID user;
    private Instant timestamp;
    private LocationData location;
    private String text;
    private int status;
    private boolean read;
    private int tier;
    private long discordMsgId;
    private Collection<CommentData> comments;

    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public UUID getUser() {
        return user;
    }
    
    public void setUser(UUID user) {
        this.user = user;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public LocationData getLocation() {
        return location;
    }
    
    public void setLocation(LocationData location) {
        this.location = location;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public int getStatus() {
        return status;
    }
    
    public void setStatus(int status) {
        this.status = status;
    }

    public int getTier() { return tier; }

    public void setTier(int tier) { this.tier = tier; }
    
    public boolean isRead() {
        return read;
    }
    
    public void setRead(boolean read) {
        this.read = read;
    }
    
    public Collection<CommentData> getComments() {
        return comments;
    }
    
    public void setComments(Collection<CommentData> comments) {
        this.comments = comments;
    }
    
    @Override
    public int compareTo(TicketData o) {
        return Objects.compare(getId(), o.getId(), Integer::compareTo);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        TicketData ticket = (TicketData) obj;
        return Objects.equals(getId(), ticket.getId());
    }


    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    public long getDiscordMsgId() {
        return discordMsgId;
    }

    public void setDiscordMsgId(long discordMsgId) {
        this.discordMsgId = discordMsgId;
    }
}

