<?php

declare(strict_types=1);

namespace App\Policies;

use App\Models\Kost;
use App\Models\User;

/**
 * Centralizes the "does this owner actually own this kost" check (BR-06)
 * so it is expressed once and reused by both the controller and, where
 * useful, form requests — instead of being re-implemented per endpoint.
 */
class KostPolicy
{
    public function update(User $user, Kost $kost): bool
    {
        return $user->id === $kost->owner_id;
    }

    public function delete(User $user, Kost $kost): bool
    {
        return $user->id === $kost->owner_id;
    }

    public function view(User $user, Kost $kost): bool
    {
        return $user->id === $kost->owner_id;
    }
}
