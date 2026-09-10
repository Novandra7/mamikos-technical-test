<?php

declare(strict_types=1);

namespace Tests\Feature\Inquiry;

use App\Events\AvailabilityInquiryCreated;
use App\Models\Kost;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Event;
use Laravel\Sanctum\Sanctum;
use Tests\Concerns\CreatesUsers;
use Tests\TestCase;

class CreateInquiryTest extends TestCase
{
    use CreatesUsers, RefreshDatabase;

    public function test_regular_user_with_enough_credit_can_inquire(): void
    {
        $kost = Kost::factory()->create(['available_rooms' => 4, 'total_rooms' => 12]);
        Sanctum::actingAs($this->createRegular(balance: 20), ['*']);

        $response = $this->postJson("/api/v1/kosts/{$kost->id}/availability-inquiries", ['message' => 'Masih ada kamar?']);

        $response->assertCreated()
            ->assertJsonPath('data.availability.disclosed', true)
            ->assertJsonPath('data.availability.available_rooms', 4)
            ->assertJsonPath('data.credit.balance_before', 20)
            ->assertJsonPath('data.credit.balance_after', 15);
    }

    public function test_premium_user_with_enough_credit_can_inquire(): void
    {
        $kost = Kost::factory()->create();
        Sanctum::actingAs($this->createPremium(balance: 40), ['*']);

        $this->postJson("/api/v1/kosts/{$kost->id}/availability-inquiries", [])
            ->assertCreated()
            ->assertJsonPath('data.credit.balance_after', 35);
    }

    public function test_insufficient_balance_is_rejected_without_side_effects(): void
    {
        $kost = Kost::factory()->create();
        $user = $this->createRegular(balance: 3);
        Sanctum::actingAs($user, ['*']);

        $response = $this->postJson("/api/v1/kosts/{$kost->id}/availability-inquiries", []);

        $response->assertStatus(422)->assertJsonPath('code', 'INSUFFICIENT_CREDIT');
        $this->assertSame(3, $user->creditBalance->fresh()->balance);
        $this->assertDatabaseCount('availability_inquiries', 0);
    }

    public function test_unauthenticated_request_is_rejected(): void
    {
        $kost = Kost::factory()->create();

        $this->postJson("/api/v1/kosts/{$kost->id}/availability-inquiries", [])
            ->assertStatus(401)->assertJsonPath('code', 'UNAUTHENTICATED');
    }

    public function test_owner_role_is_forbidden_from_inquiring(): void
    {
        $kost = Kost::factory()->create();
        Sanctum::actingAs(User::factory()->owner()->create(), ['*']);

        $this->postJson("/api/v1/kosts/{$kost->id}/availability-inquiries", [])->assertStatus(403);
    }

    public function test_nonexistent_kost_returns_not_found_and_leaves_balance_untouched(): void
    {
        $user = $this->createRegular(balance: 20);
        Sanctum::actingAs($user, ['*']);

        $this->postJson('/api/v1/kosts/999999/availability-inquiries', [])->assertStatus(404);
        $this->assertSame(20, $user->creditBalance->fresh()->balance);
    }

    public function test_inactive_kost_returns_not_found_and_leaves_balance_untouched(): void
    {
        $kost = Kost::factory()->inactive()->create();
        $user = $this->createRegular(balance: 20);
        Sanctum::actingAs($user, ['*']);

        $this->postJson("/api/v1/kosts/{$kost->id}/availability-inquiries", [])->assertStatus(404);
        $this->assertSame(20, $user->creditBalance->fresh()->balance);
    }

    public function test_successful_inquiry_creates_exactly_one_ledger_row(): void
    {
        $kost = Kost::factory()->create();
        Sanctum::actingAs($this->createRegular(balance: 20), ['*']);

        $this->postJson("/api/v1/kosts/{$kost->id}/availability-inquiries", [])->assertCreated();

        $this->assertDatabaseCount('credit_transactions', 1);
    }

    public function test_event_is_dispatched_on_success(): void
    {
        Event::fake();

        $kost = Kost::factory()->create();
        Sanctum::actingAs($this->createRegular(balance: 20), ['*']);

        $this->postJson("/api/v1/kosts/{$kost->id}/availability-inquiries", [])->assertCreated();

        Event::assertDispatched(AvailabilityInquiryCreated::class);
    }
}
