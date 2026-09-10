<?php

declare(strict_types=1);

namespace Tests\Unit\Services;

use App\DTOs\Kost\UpdateKostData;
use App\Exceptions\Domain\NotKostOwnerException;
use App\Models\Kost;
use App\Models\User;
use App\Services\KostService;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Validation\ValidationException;
use Tests\TestCase;

class KostServiceTest extends TestCase
{
    use RefreshDatabase;

    private KostService $kostService;

    protected function setUp(): void
    {
        parent::setUp();

        $this->kostService = app(KostService::class);
    }

    public function test_updating_someone_elses_kost_throws(): void
    {
        $ownerA = User::factory()->owner()->create();
        $ownerB = User::factory()->owner()->create();
        $kost = Kost::factory()->forOwner($ownerB)->create();

        $this->expectException(NotKostOwnerException::class);

        $this->kostService->update($kost->id, $ownerA->id, UpdateKostData::fromArray(['price_per_month' => 1_000_000]));
    }

    public function test_available_rooms_cannot_exceed_total_rooms_when_only_one_field_is_sent(): void
    {
        $owner = User::factory()->owner()->create();
        $kost = Kost::factory()->forOwner($owner)->create(['total_rooms' => 5, 'available_rooms' => 2]);

        $this->expectException(ValidationException::class);

        $this->kostService->update($kost->id, $owner->id, UpdateKostData::fromArray(['available_rooms' => 9]));
    }

    public function test_delete_is_a_soft_delete(): void
    {
        $owner = User::factory()->owner()->create();
        $kost = Kost::factory()->forOwner($owner)->create();

        $this->kostService->delete($kost->id, $owner->id);

        $this->assertSoftDeleted('kosts', ['id' => $kost->id]);
    }
}
