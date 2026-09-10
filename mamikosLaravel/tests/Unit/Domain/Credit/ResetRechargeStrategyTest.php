<?php

declare(strict_types=1);

namespace Tests\Unit\Domain\Credit;

use App\Domain\Credit\Strategies\ResetRechargeStrategy;
use PHPUnit\Framework\Attributes\DataProvider;
use Tests\TestCase;

class ResetRechargeStrategyTest extends TestCase
{
    #[DataProvider('balances')]
    public function test_it_resets_balance_to_quota(int $before, int $quota): void
    {
        $strategy = new ResetRechargeStrategy;

        $this->assertSame($quota, $strategy->apply($before, $quota, maxBalance: 200));
    }

    public static function balances(): array
    {
        return [
            'below quota' => [3, 20],
            'above quota' => [35, 20],
            'exactly at quota' => [20, 20],
            'zero balance' => [0, 40],
        ];
    }
}
