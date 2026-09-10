<?php

declare(strict_types=1);

namespace Tests\Unit\Domain\Credit;

use App\Domain\Credit\Strategies\TopUpRechargeStrategy;
use Tests\TestCase;

class TopUpRechargeStrategyTest extends TestCase
{
    public function test_it_adds_quota_on_top_of_existing_balance(): void
    {
        $strategy = new TopUpRechargeStrategy;

        $this->assertSame(35, $strategy->apply(balanceBefore: 15, quota: 20, maxBalance: 200));
    }

    public function test_it_caps_the_result_at_max_balance(): void
    {
        $strategy = new TopUpRechargeStrategy;

        $this->assertSame(200, $strategy->apply(balanceBefore: 190, quota: 40, maxBalance: 200));
    }
}
