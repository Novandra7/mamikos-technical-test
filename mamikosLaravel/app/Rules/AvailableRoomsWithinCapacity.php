<?php

declare(strict_types=1);

namespace App\Rules;

use Closure;
use Illuminate\Contracts\Validation\ValidationRule;

/**
 * BR-12: `available_rooms` can never exceed `total_rooms`. Implemented as
 * a reusable rule (rather than an inline closure) so both StoreKostRequest
 * and UpdateKostRequest can share it.
 */
final class AvailableRoomsWithinCapacity implements ValidationRule
{
    public function __construct(private readonly int|string|null $totalRooms) {}

    public function validate(string $attribute, mixed $value, Closure $fail): void
    {
        if ($this->totalRooms === null) {
            return;
        }

        if ((int) $value > (int) $this->totalRooms) {
            $fail('The :attribute must not exceed the total number of rooms.');
        }
    }
}
