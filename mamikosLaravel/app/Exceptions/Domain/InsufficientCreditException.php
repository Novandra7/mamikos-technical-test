<?php

declare(strict_types=1);

namespace App\Exceptions\Domain;

/**
 * BR-03 / D-04: thrown when a wallet does not have enough balance to cover
 * an inquiry. Maps to 422 rather than 402 — see D-04 for the rationale.
 */
final class InsufficientCreditException extends DomainException
{
    public function __construct(private readonly int $required, private readonly int $current)
    {
        parent::__construct('Your credit balance is not enough to ask about room availability');
    }

    public function httpStatus(): int
    {
        return 422;
    }

    public function errorCode(): string
    {
        return 'INSUFFICIENT_CREDIT';
    }

    public function errors(): array
    {
        return [
            'credit' => ["Required {$this->required} credit, current balance {$this->current}"],
        ];
    }
}
