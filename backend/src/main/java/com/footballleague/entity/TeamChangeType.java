package com.footballleague.entity;

/**
 * RELEGATED: 1. Lig'den 2. Lig'e düştü; PROMOTED: 2. Lig'den 1. Lig'e çıktı; DROPPED_OUT: 2. Lig'in sonuncularından
 * olup lig sisteminden ayrıldı (arşivlendi); JOINED: 2. Lig'e yeni katıldı.
 */
public enum TeamChangeType {
    RELEGATED,
    PROMOTED,
    DROPPED_OUT,
    JOINED
}
