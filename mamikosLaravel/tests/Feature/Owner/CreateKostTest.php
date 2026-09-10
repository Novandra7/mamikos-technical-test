<?php

declare(strict_types=1);

namespace Tests\Feature\Owner;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class CreateKostTest extends TestCase
{
    use RefreshDatabase;

    private array $payload = [
        'name' => 'Kost Melati Residence',
        'description' => 'Kost putri eksklusif dekat kampus.',
        'address' => [
            'street' => 'Jl. Kaliurang KM 5',
            'district' => 'Depok',
            'city' => 'Sleman',
            'province' => 'DI Yogyakarta',
        ],
        'price_per_month' => 950000,
        'room_type' => 'putri',
        'total_rooms' => 12,
        'available_rooms' => 4,
        'facilities' => ['wifi', 'kamar mandi dalam'],
    ];

    public function test_owner_can_create_a_kost(): void
    {
        Sanctum::actingAs($owner = User::factory()->owner()->create(), ['*']);

        $response = $this->postJson('/api/v1/owner/kosts', $this->payload);

        $response->assertCreated()
            ->assertJsonPath('data.name', 'Kost Melati Residence')
            ->assertJsonPath('data.available_rooms', 4);

        $this->assertDatabaseHas('kosts', ['owner_id' => $owner->id, 'name' => 'Kost Melati Residence']);
    }

    public function test_owner_can_create_more_than_one_kost(): void
    {
        Sanctum::actingAs($owner = User::factory()->owner()->create(), ['*']);

        $this->postJson('/api/v1/owner/kosts', $this->payload)->assertCreated();
        $this->postJson('/api/v1/owner/kosts', [...$this->payload, 'name' => 'Kost Kedua'])->assertCreated();

        $this->assertDatabaseCount('kosts', 2);
    }

    public function test_non_owner_roles_are_forbidden(): void
    {
        Sanctum::actingAs(User::factory()->regular()->create(), ['*']);

        $this->postJson('/api/v1/owner/kosts', $this->payload)->assertStatus(403);
    }

    public function test_client_supplied_owner_id_is_ignored(): void
    {
        Sanctum::actingAs($owner = User::factory()->owner()->create(), ['*']);
        $someoneElse = User::factory()->owner()->create();

        $response = $this->postJson('/api/v1/owner/kosts', [...$this->payload, 'owner_id' => $someoneElse->id]);

        $response->assertCreated();
        $this->assertDatabaseHas('kosts', ['owner_id' => $owner->id]);
        $this->assertDatabaseMissing('kosts', ['owner_id' => $someoneElse->id]);
    }

    public function test_available_rooms_exceeding_total_rooms_is_rejected(): void
    {
        Sanctum::actingAs(User::factory()->owner()->create(), ['*']);

        $response = $this->postJson('/api/v1/owner/kosts', [...$this->payload, 'total_rooms' => 5, 'available_rooms' => 10]);

        $response->assertStatus(422)->assertJsonPath('code', 'VALIDATION_ERROR');
    }

    public function test_missing_required_fields_are_rejected(): void
    {
        Sanctum::actingAs(User::factory()->owner()->create(), ['*']);

        $response = $this->postJson('/api/v1/owner/kosts', ['name' => 'Incomplete']);

        $response->assertStatus(422);
    }
}
