<?php

declare(strict_types=1);

namespace App\Domain\Credit\Strategies;

/**
 * Default strategy (D-01, BR-08): the balance is reset to the full role
 * quota, regardless of what was left over from the previous month.
 */
final class ResetRechargeStrategy implements RechargeStrategyInterface
{
    public function apply(int $balanceBefore, int $quota, int $maxBalance): int
    {
        return $quota;
    }
}
