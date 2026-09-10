<?php

declare(strict_types=1);

namespace App\Services;

use App\DTOs\Kost\CreateKostData;
use App\DTOs\Kost\UpdateKostData;
use App\Exceptions\Domain\NotKostOwnerException;
use App\Models\Kost;
use App\Repositories\Contracts\KostRepositoryInterface;
use Illuminate\Contracts\Pagination\LengthAwarePaginator;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\ValidationException;

/**
 * Owner-facing kost CRUD. Ownership is always re-checked here — even
 * though the route only carries a route-model-bound Kost — so a stale
 * client can never bypass BR-06 by guessing another owner's kost id.
 */
final class KostService
{
    public function __construct(private readonly KostRepositoryInterface $kosts) {}

    public function create(CreateKostData $data): Kost
    {
        return DB::transaction(function () use ($data): Kost {
            $kost = $this->kosts->create([
                'owner_id' => $data->ownerId,
                'name' => $data->name,
                'description' => $data->description,
                'address_street' => $data->address->street,
                'address_district' => $data->address->district,
                'address_city' => $data->address->city,
                'address_province' => $data->address->province,
                'postal_code' => $data->address->postalCode,
                'latitude' => $data->latitude,
                'longitude' => $data->longitude,
                'price_per_month' => $data->pricePerMonth,
                'room_type' => $data->roomType,
                'total_rooms' => $data->totalRooms,
                'available_rooms' => $data->availableRooms,
                'is_active' => $data->isActive,
            ]);

            $this->syncFacilities($kost, $data->facilities);
            $this->syncPhotos($kost, $data->photos);

            return $kost->fresh(['facilities', 'photos', 'owner']);
        });
    }

    /**
     * @param  array<string, mixed>  $filters
     * @return LengthAwarePaginator<int, Kost>
     */
    public function listForOwner(int $ownerId, array $filters, int $page, int $perPage): LengthAwarePaginator
    {
        return $this->kosts->paginateForOwner($ownerId, $filters, $page, $perPage);
    }

    public function findOwned(int $kostId, int $ownerId): Kost
    {
        $kost = $this->kosts->findAnyOrFail($kostId);

        $this->assertOwnership($kost, $ownerId);

        return $kost;
    }

    public function update(int $kostId, int $ownerId, UpdateKostData $data): Kost
    {
        return DB::transaction(function () use ($kostId, $ownerId, $data): Kost {
            $kost = $this->kosts->findAnyOrFail($kostId);
            $this->assertOwnership($kost, $ownerId);

            $attributes = $data->toKostAttributes();
            $this->assertAvailableRoomsWithinCapacity($kost, $attributes);

            $kost = $this->kosts->update($kost, $attributes);

            if ($data->facilities !== null) {
                $this->syncFacilities($kost, $data->facilities);
            }

            if ($data->photos !== null) {
                $this->syncPhotos($kost, $data->photos);
            }

            return $kost->fresh(['facilities', 'photos', 'owner']);
        });
    }

    public function delete(int $kostId, int $ownerId): void
    {
        $kost = $this->kosts->findAnyOrFail($kostId);
        $this->assertOwnership($kost, $ownerId);

        $this->kosts->delete($kost);
    }

    /**
     * BR-12, enforced here so it applies uniformly to both PUT (full
     * payload) and PATCH (partial payload) instead of duplicating the
     * check per-request-shape.
     *
     * @param  array<string, mixed>  $attributes
     */
    private function assertAvailableRoomsWithinCapacity(Kost $kost, array $attributes): void
    {
        $totalRooms = $attributes['total_rooms'] ?? $kost->total_rooms;
        $availableRooms = $attributes['available_rooms'] ?? $kost->available_rooms;

        if ($availableRooms > $totalRooms) {
            throw ValidationException::withMessages([
                'available_rooms' => ['The available rooms must not exceed the total number of rooms.'],
            ]);
        }
    }

    /**
     * @throws NotKostOwnerException
     */
    private function assertOwnership(Kost $kost, int $ownerId): void
    {
        if ($kost->owner_id !== $ownerId) {
            throw new NotKostOwnerException;
        }
    }

    /**
     * @param  list<string>  $facilities
     */
    private function syncFacilities(Kost $kost, array $facilities): void
    {
        $kost->facilities()->delete();

        foreach ($facilities as $facility) {
            $kost->facilities()->create(['name' => $facility]);
        }
    }

    /**
     * @param  list<string>  $photos
     */
    private function syncPhotos(Kost $kost, array $photos): void
    {
        $kost->photos()->delete();

        foreach ($photos as $index => $url) {
            $kost->photos()->create(['url' => $url, 'sort_order' => $index]);
        }
    }
}
