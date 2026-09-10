<?php

declare(strict_types=1);

namespace App\Domain\Credit\Strategies;

/**
 * Alternative strategy (BR-08): the quota is added on top of the existing
 * balance instead of resetting it, capped at `credit.max_balance`.
 */
final class TopUpRechargeStrategy implements RechargeStrategyInterface
{
    public function apply(int $balanceBefore, int $quota, int $maxBalance): int
    {
        return min($balanceBefore + $quota, $maxBalance);
    }
}
