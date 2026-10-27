#!/usr/bin/env bash
# Yayındaki veritabanını (Render Postgres) başka bir Postgres'e (ör. Neon) birebir kopyalar:
# tüm şemalar (public + her oda için room_xxx) ve veriler.
#
# Kullanım:
#   KAYNAK_URL='postgresql://kullanici:sifre@render-host/futbol_ligi' \
#   HEDEF_URL='postgresql://kullanici:sifre@neon-host/neondb?sslmode=require' \
#   scripts/veritabani-tasi.sh
#
# Render: futbol-ligi-db → Connect → External Database URL. Neon: Dashboard → Connection string.
# Gerekenler: pg_dump / pg_restore 16+ (macOS: brew install postgresql@16).
# Taşıma sırasında maç oynanmaması için önce Render'da servisi askıya al (Suspend), bitince yeni
# bilgileri gir ve servisi yeniden başlat.
set -euo pipefail
: "${KAYNAK_URL:?KAYNAK_URL gerekli}"
: "${HEDEF_URL:?HEDEF_URL gerekli}"

DOSYA="$(mktemp -d)/futbol-ligi.dump"
echo "Kaynak dökülüyor → $DOSYA"
pg_dump --format=custom --no-owner --no-privileges --dbname="$KAYNAK_URL" --file="$DOSYA"

echo "Hedefe yükleniyor"
pg_restore --no-owner --no-privileges --clean --if-exists --exit-on-error --dbname="$HEDEF_URL" "$DOSYA"

echo "Karşılaştırma (oda sayısı):"
psql "$KAYNAK_URL" -tAc "select count(*) from rooms"
psql "$HEDEF_URL" -tAc "select count(*) from rooms"
