/*
 * Copyright 2019 Alex Thomson
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

package io.github.lxgaming.ticket.common.storage;

import io.github.lxgaming.ticket.api.data.CommentData;
import io.github.lxgaming.ticket.api.data.LocationData;
import io.github.lxgaming.ticket.api.data.TicketData;
import io.github.lxgaming.ticket.api.data.UserData;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

public interface Query {
    
    boolean createTables();
    
    CommentData createComment(int ticketId, UUID uniqueId, Instant timestamp, String text) throws Exception;
    
    TicketData createTicket(UUID uniqueId, Instant timestamp, LocationData location, String text, int tier) throws Exception;
    
    UserData createUser(UUID uniqueId, String name) throws Exception;
    
    Collection<CommentData> getComments(int ticketId) throws Exception;
    
    TicketData getTicket(int ticketId) throws Exception;
    
    Collection<Integer> getOpenTickets() throws Exception;
    
    Collection<Integer> getUnreadTickets(UUID uniqueId) throws Exception;
    
    UserData getUser(UUID uniqueId) throws Exception;
    
    Collection<UUID> getUsers(String name) throws Exception;
    
    boolean updateTicket(TicketData ticket);
    
    boolean updateUser(UserData user);

    TicketData getTicketByDiscordMessageID(long discordId) throws SQLException;
}