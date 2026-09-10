<?php

declare(strict_types=1);

namespace App\Policies;

use App\Models\AvailabilityInquiry;
use App\Models\User;

/**
 * BR-06: only the owner of the kost that received the inquiry may see or
 * reply to it.
 */
class InquiryPolicy
{
    public function reply(User $user, AvailabilityInquiry $inquiry): bool
    {
        return $user->id === $inquiry->kost->owner_id;
    }

    public function view(User $user, AvailabilityInquiry $inquiry): bool
    {
        return $user->id === $inquiry->kost->owner_id;
    }
}
