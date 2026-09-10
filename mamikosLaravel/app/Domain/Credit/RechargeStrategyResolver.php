<?php

declare(strict_types=1);

namespace App\Domain\Credit;

use App\Domain\Credit\Strategies\RechargeStrategyInterface;
use App\Domain\Credit\Strategies\ResetRechargeStrategy;
use App\Domain\Credit\Strategies\TopUpRechargeStrategy;
use App\Enums\RechargeStrategyType;

final class RechargeStrategyResolver
{
    public static function resolve(RechargeStrategyType $type): RechargeStrategyInterface
    {
        return match ($type) {
            RechargeStrategyType::RESET => new ResetRechargeStrategy,
            RechargeStrategyType::TOPUP => new TopUpRechargeStrategy,
        };
    }
}
