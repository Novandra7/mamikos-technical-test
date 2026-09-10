<?php

declare(strict_types=1);

namespace Tests\Console;

use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\Concerns\CreatesUsers;
use Tests\TestCase;

class RechargeUserCreditsCommandTest extends TestCase
{
    use CreatesUsers, RefreshDatabase;

    public function test_it_recharges_a_single_user_via_option(): void
    {
        $user = $this->createRegular(balance: 3);
        $user->creditBalance->update(['last_recharged_at' => now()->subMonthNoOverflow()]);

        $this->artisan('credits:recharge', ['--user-id' => $user->id])->assertExitCode(0);

        $this->assertSame(20, $user->creditBalance->fresh()->balance);
    }

    public function test_it_is_idempotent_when_run_twice(): void
    {
        $user = $this->createRegular(balance: 3);
        $user->creditBalance->update(['last_recharged_at' => now()->subMonthNoOverflow()]);

        $this->artisan('credits:recharge', ['--force' => true])->assertExitCode(0);
        $this->assertSame(20, $user->creditBalance->fresh()->balance);

        $this->artisan('credits:recharge')->assertExitCode(0);
        $this->assertSame(20, $user->creditBalance->fresh()->balance);
        $this->assertDatabaseCount('credit_transactions', 1);
    }

    public function test_dry_run_option_does_not_persist_changes(): void
    {
        $user = $this->createRegular(balance: 3);
        $user->creditBalance->update(['last_recharged_at' => now()->subMonthNoOverflow()]);

        $this->artisan('credits:recharge', ['--dry-run' => true])->assertExitCode(0);

        $this->assertSame(3, $user->creditBalance->fresh()->balance);
    }
}
