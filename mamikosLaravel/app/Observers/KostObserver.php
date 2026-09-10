<?php

declare(strict_types=1);

namespace App\Observers;

use App\Models\Kost;

/**
 * Normalizes free-text fields before they ever reach the database, so the
 * search filter (address_city LIKE …) does not have to compensate for
 * inconsistent casing/whitespace typed by different owners.
 */
class KostObserver
{
    public function saving(Kost $kost): void
    {
        $kost->name = trim($kost->name);
        $kost->address_city = $this->toTitleCase($kost->address_city);
        $kost->address_district = $this->toTitleCase($kost->address_district);
        $kost->address_street = trim($kost->address_street);
    }

    private function toTitleCase(string $value): string
    {
        return mb_convert_case(trim($value), MB_CASE_TITLE, 'UTF-8');
    }
}
