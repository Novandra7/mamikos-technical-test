<?php

declare(strict_types=1);

namespace App\Domain\Credit\Strategies;

interface RechargeStrategyInterface
{
    /**
     * Compute the new balance for a monthly recharge.
     */
    public function apply(int $balanceBefore, int $quota, int $maxBalance): int;
}
