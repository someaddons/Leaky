package com.leaky.storage;

public enum DetectionSource
{
    ITEM_TICK(5),
    ENTITY_SECTION(16);

    public final int range;

    DetectionSource(final int i)
    {
        range = i;
    }
}
