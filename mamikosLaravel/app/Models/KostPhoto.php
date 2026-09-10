<?php

declare(strict_types=1);

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/**
 * @property int $id
 * @property int $kost_id
 * @property string $url
 * @property int $sort_order
 */
class KostPhoto extends Model
{
    protected $fillable = [
        'kost_id',
        'url',
        'sort_order',
    ];

    /**
     * @return BelongsTo<Kost, $this>
     */
    public function kost(): BelongsTo
    {
        return $this->belongsTo(Kost::class);
    }
}
