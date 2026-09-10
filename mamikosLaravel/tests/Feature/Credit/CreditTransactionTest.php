<?php

declare(strict_types=1);

namespace Tests\Feature\Credit;

use App\Models\Kost;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\Concerns\CreatesUsers;
use Tests\TestCase;

class CreditTransactionTest extends TestCase
{
    use CreatesUsers, RefreshDatabase;

    public function test_ledger_shows_initial_grant_then_deduction_newest_first(): void
    {
        $user = $this->createRegular(balance: 20);
        $user->creditTransactions()->create([
            'type' => 'INITIAL_GRANT',
            'amount' => 20,
            'balance_before' => 0,
            'balance_after' => 20,
        ]);

        $kost = Kost::factory()->create();
        Sanctum::actingAs($user, ['*']);
        $this->postJson("/api/v1/kosts/{$kost->id}/availability-inquiries", [])->assertCreated();

        $response = $this->getJson('/api/v1/me/credits/transactions');

        $response->assertOk();
        $this->assertSame('INQUIRY_DEDUCTION', $response->json('data.0.type'));
        $this->assertSame('INITIAL_GRANT', $response->json('data.1.type'));
    }

    public function test_transactions_are_paginated(): void
    {
        $user = $this->createRegular(balance: 100);

        for ($i = 0; $i < 15; $i++) {
            $user->creditTransactions()->create([
                'type' => 'ADJUSTMENT', 'amount' => 0, 'balance_before' => 100, 'balance_after' => 100,
            ]);
        }

        Sanctum::actingAs($user, ['*']);

        $response = $this->getJson('/api/v1/me/credits/transactions');

        $response->assertOk()->assertJsonCount(10, 'data');
        $this->assertSame(15, $response->json('meta.pagination.total'));
    }
}
