<?php

declare(strict_types=1);

namespace App\Domain\Shared;

use InvalidArgumentException;

/**
 * Encapsulates a positive IDR amount stored with two decimal places.
 *
 * Money never stores a float internally — that would reintroduce the
 * rounding problems `DECIMAL(12,2)` (D-07) is meant to avoid. Everything is
 * kept as an integer number of "cents" (rupiah sen) and only formatted to a
 * decimal string at the edges.
 */
final readonly class Money
{
    private int $cents;

    private function __construct(int $cents)
    {
        if ($cents <= 0) {
            throw new InvalidArgumentException('Money amount must be greater than zero.');
        }

        $this->cents = $cents;
    }

    public static function fromRupiah(int|float|string $rupiah): self
    {
        return new self((int) round(((float) $rupiah) * 100));
    }

    public function toDecimalString(): string
    {
        return number_format($this->cents / 100, 2, '.', '');
    }

    public function toFloat(): float
    {
        return $this->cents / 100;
    }

    public function equals(self $other): bool
    {
        return $this->cents === $other->cents;
    }
}
