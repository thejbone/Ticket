package io.github.lxgaming.ticket.api.data;

import java.util.Comparator;

public class SortByTicketTier implements Comparator<TicketData> {
    @Override
    public int compare(TicketData o1, TicketData o2) {
        return o2.getTier() - o1.getTier();
    }
}
