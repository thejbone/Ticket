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

package io.github.lxgaming.ticket.common.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Sets;
import io.github.lxgaming.ticket.api.Ticket;
import io.github.lxgaming.ticket.api.data.CommentData;
import io.github.lxgaming.ticket.api.data.LocationData;
import io.github.lxgaming.ticket.api.data.TicketData;
import io.github.lxgaming.ticket.api.data.UserData;
import io.github.lxgaming.ticket.common.TicketImpl;
import io.github.lxgaming.ticket.common.cache.TicketExpiry;
import io.github.lxgaming.ticket.common.cache.UserExpiry;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public class DataManager {
    
    private static final Cache<Integer, TicketData> TICKET_CACHE = Caffeine.newBuilder().expireAfter(new TicketExpiry()).build();
    private static final Cache<UUID, UserData> USER_CACHE = Caffeine.newBuilder().expireAfter(new UserExpiry()).build();
    
    public static Optional<UserData> getCachedUser(UUID uniqueId) {
        return Optional.ofNullable(getUserCache().getIfPresent(uniqueId));
    }
    
    public static Optional<UserData> getUser(UUID uniqueId) {
        return Optional.ofNullable(getUserCache().get(uniqueId, key -> {
            try {
                return TicketImpl.getInstance().getStorage().getQuery().getUser(key);
            } catch (Exception ex) {
                Ticket.getInstance().getLogger().error("Encountered an error processing DataManager::getUser", ex);
                return null;
            }
        }));
    }
    
    public static Optional<Collection<UserData>> getUsers(String name) {
        try {
            Collection<UUID> uniqueIds = TicketImpl.getInstance().getStorage().getQuery().getUsers(name);
            if (uniqueIds == null || uniqueIds.isEmpty()) {
                return Optional.empty();
            }
            
            Collection<UserData> users = Sets.newHashSet();
            uniqueIds.forEach(uniqueId -> getUser(uniqueId).map(users::add));
            return Optional.of(users);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
    
    public static Optional<UserData> getOrCreateUser(UUID uniqueId) {
        return Optional.ofNullable(getUserCache().get(uniqueId, key -> {
            try {
                UserData user = TicketImpl.getInstance().getStorage().getQuery().getUser(key);
                if (user != null) {
                    return user;
                }
                return TicketImpl.getInstance().getStorage().getQuery().createUser(key, "Unknown");
            } catch (Exception ex) {
                Ticket.getInstance().getLogger().error("Encountered an error processing DataManager::getOrCreateUser", ex);
                return null;
            }
        }));
    }

    public static Optional<UserData> getOrCreateUserName(UUID uniqueId, String name) {
        return Optional.ofNullable(getUserCache().get(uniqueId, key -> {
            try {
                UserData user = TicketImpl.getInstance().getStorage().getQuery().getUser(key);
                if (user != null) {
                    return user;
                }
                return TicketImpl.getInstance().getStorage().getQuery().createUser(key, name);
            } catch (Exception ex) {
                Ticket.getInstance().getLogger().error("Encountered an error processing DataManager::getOrCreateUser", ex);
                return null;
            }
        }));
    }
    
    public static Collection<TicketData> getCachedOpenTickets(UUID uniqueId) {
        Collection<TicketData> tickets = Sets.newTreeSet();
        for (TicketData ticket : getTicketCache().asMap().values()) {
            if (ticket.getUser().equals(uniqueId) && ticket.getStatus() == 0) {
                getCachedTicket(ticket.getId()).ifPresent(tickets::add);
            }
        }
        
        return tickets;
    }
    
    public static Collection<TicketData> getCachedOpenTickets() {
        Collection<TicketData> tickets = Sets.newTreeSet();
        for (TicketData ticket : getTicketCache().asMap().values()) {
            if (ticket.getStatus() == 0) {
                getCachedTicket(ticket.getId()).ifPresent(tickets::add);
            }
        }
        
        return tickets;
    }
    public static Collection<TicketData> getCachedOpenTicketsByTier(int tier) {
        Collection<TicketData> tickets = Sets.newTreeSet();
        for (TicketData ticket : getTicketCache().asMap().values()) {
            if (ticket.getStatus() == 0 && ticket.getTier() == tier) {
                getCachedTicket(ticket.getId()).ifPresent(tickets::add);
            }
        }
        return tickets;
    }

    public static Optional<TicketData> getByDiscordMessageID(long discordId) {
        try {
            TicketData ticket = TicketImpl.getInstance().getStorage().getQuery().getTicketByDiscordMessageID(discordId);
            Optional<TicketData> ticketData;
            if(ticket != null){
                ticketData = getCachedTicket(ticket.getId());
                if (!ticketData.isPresent()) {
                    ticketData = Optional.of(ticket);
                }
                Collection<CommentData> comments = TicketImpl.getInstance().getStorage().getQuery().getComments(ticketData.get().getId());
                if (comments != null) {
                    ticketData.get().setComments(comments);
                }
                return ticketData;
            }
        } catch (Exception e){
            Ticket.getInstance().getLogger().error("Encountered an error processing DataManager::getByDiscordMessageID", e);
            return Optional.empty();
        }
        return Optional.empty();
    }

    public static Collection<TicketData> getCachedUnreadTickets(UUID uniqueId) {
        Collection<TicketData> tickets = Sets.newTreeSet();
        for (TicketData ticket : getTicketCache().asMap().values()) {
            if (ticket.getUser().equals(uniqueId) && ticket.getStatus() == 1 && !ticket.isRead()) {
                getCachedTicket(ticket.getId()).ifPresent(tickets::add);
            }
        }
        
        return tickets;
    }
    
    public static Optional<TicketData> getCachedTicket(int ticketId) {
        return Optional.ofNullable(getTicketCache().getIfPresent(ticketId));
    }
    
    public static Optional<Collection<TicketData>> getOpenTickets() {
        try {
            Collection<Integer> ticketIds = TicketImpl.getInstance().getStorage().getQuery().getOpenTickets();
            if (ticketIds == null || ticketIds.isEmpty()) {
                return Optional.empty();
            }
            
            Collection<TicketData> tickets = Sets.newTreeSet();
            ticketIds.forEach(ticketId -> getTicket(ticketId).map(tickets::add));
            return Optional.of(tickets);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
    
    public static Optional<Collection<TicketData>> getUnreadTickets(UUID uniqueId) {
        try {
            Collection<Integer> ticketIds = TicketImpl.getInstance().getStorage().getQuery().getUnreadTickets(uniqueId);
            if (ticketIds == null || ticketIds.isEmpty()) {
                return Optional.empty();
            }
            
            Collection<TicketData> tickets = Sets.newTreeSet();
            ticketIds.forEach(ticketId -> getTicket(ticketId).map(tickets::add));
            return Optional.of(tickets);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
    
    public static Optional<TicketData> getTicket(int ticketId) {
        return Optional.ofNullable(getTicketCache().get(ticketId, key -> {
            try {
                TicketData ticket = TicketImpl.getInstance().getStorage().getQuery().getTicket(key);
                if (ticket != null) {
                    Collection<CommentData> comments = TicketImpl.getInstance().getStorage().getQuery().getComments(ticket.getId());
                    if (comments != null) {
                        ticket.setComments(comments);
                    }
                }
                
                return ticket;
            } catch (Exception ex) {
                Ticket.getInstance().getLogger().error("Encountered an error processing DataManager::getTicket", ex);
                return null;
            }
        }));
    }
    
    public static Optional<TicketData> createTicket(UUID uniqueId, Instant timestamp, LocationData location, String text, int tier) {
        try {
            TicketData ticket = TicketImpl.getInstance().getStorage().getQuery().createTicket(uniqueId, timestamp, location, text, tier);
            getTicketCache().put(ticket.getId(), ticket);
            return Optional.of(ticket);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
    
    public static Optional<CommentData> createComment(int ticketId, UUID uniqueId, Instant timestamp, String text) {
        try {
            TicketData ticket = getTicket(ticketId).orElse(null);
            if (ticket == null) {
                return Optional.empty();
            }
            CommentData comment = TicketImpl.getInstance().getStorage().getQuery().createComment(ticketId, uniqueId, timestamp, text);
            ticket.getComments().add(comment);
            return Optional.of(comment);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
    
    public static Cache<Integer, TicketData> getTicketCache() {
        return TICKET_CACHE;
    }
    
    public static Cache<UUID, UserData> getUserCache() {
        return USER_CACHE;
    }
}