<?php

declare(strict_types=1);

namespace App\Enums;

/**
 * Which policy `credits:recharge` should apply. Kept separate from the
 * Strategy pattern implementation classes (app/Domain/Credit/Strategies)
 * so the console command and config can refer to a plain, validated value.
 */
enum RechargeStrategyType: string
{
    case RESET = 'reset';
    case TOPUP = 'topup';
}
