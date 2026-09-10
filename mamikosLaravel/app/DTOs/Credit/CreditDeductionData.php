<?php

declare(strict_types=1);

namespace App\DTOs\Credit;

final readonly class CreditDeductionData
{
    public function __construct(
        public int $userId,
        public int $amount,
        public string $description,
        public ?string $referenceType = null,
        public ?int $referenceId = null,
    ) {}
}
