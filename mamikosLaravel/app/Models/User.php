<?php

declare(strict_types=1);

namespace App\Models;

use App\Enums\UserRole;
use Database\Factories\UserFactory;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Database\Eloquent\Relations\HasOne;
use Illuminate\Database\Eloquent\SoftDeletes;
use Illuminate\Foundation\Auth\User as Authenticatable;
use Illuminate\Notifications\Notifiable;
use Illuminate\Support\Carbon;
use Laravel\Sanctum\HasApiTokens;

/**
 * @property int $id
 * @property string $name
 * @property string $email
 * @property string $password
 * @property UserRole $role
 * @property string|null $phone
 * @property Carbon|null $created_at
 * @property Carbon|null $updated_at
 * @property Carbon|null $deleted_at
 * @property-read CreditBalance|null $creditBalance
 */
class User extends Authenticatable
{
    /** @use HasFactory<UserFactory> */
    use HasApiTokens, HasFactory, Notifiable, SoftDeletes;

    protected $fillable = [
        'name',
        'email',
        'password',
        'role',
        'phone',
    ];

    protected $hidden = [
        'password',
        'remember_token',
    ];

    protected function casts(): array
    {
        return [
            'email_verified_at' => 'datetime',
            'password' => 'hashed',
            'role' => UserRole::class,
        ];
    }

    /**
     * @return HasOne<CreditBalance, $this>
     */
    public function creditBalance(): HasOne
    {
        return $this->hasOne(CreditBalance::class);
    }

    /**
     * @return HasMany<CreditTransaction, $this>
     */
    public function creditTransactions(): HasMany
    {
        return $this->hasMany(CreditTransaction::class);
    }

    /**
     * Kosts owned by this user (only meaningful when role is OWNER).
     *
     * @return HasMany<Kost, $this>
     */
    public function kosts(): HasMany
    {
        return $this->hasMany(Kost::class, 'owner_id');
    }

    /**
     * Availability inquiries submitted by this user (only meaningful when
     * role is REGULAR or PREMIUM).
     *
     * @return HasMany<AvailabilityInquiry, $this>
     */
    public function inquiries(): HasMany
    {
        return $this->hasMany(AvailabilityInquiry::class);
    }

    public function isOwner(): bool
    {
        return $this->role === UserRole::OWNER;
    }
}
