<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('kosts', function (Blueprint $table) {
            $table->id();
            $table->foreignId('owner_id')->constrained('users')->cascadeOnDelete();
            $table->string('name', 150);
            $table->text('description')->nullable();
            $table->string('address_street', 255);
            $table->string('address_district', 100);
            $table->string('address_city', 100);
            $table->string('address_province', 100);
            $table->string('postal_code', 10)->nullable();
            $table->decimal('latitude', 10, 8)->nullable();
            $table->decimal('longitude', 11, 8)->nullable();
            $table->decimal('price_per_month', 12, 2);
            $table->string('room_type', 20);
            $table->integer('total_rooms');
            $table->integer('available_rooms');
            $table->boolean('is_active')->default(true);
            $table->timestamps();
            $table->softDeletes();

            $table->index(['owner_id', 'deleted_at']);
            $table->index('price_per_month');
            $table->index(['address_city', 'is_active', 'deleted_at']);
        });

        if (DB::getDriverName() === 'mysql') {
            DB::statement('ALTER TABLE kosts ADD CONSTRAINT chk_kosts_price_positive CHECK (price_per_month > 0)');
            DB::statement('ALTER TABLE kosts ADD CONSTRAINT chk_kosts_total_rooms_positive CHECK (total_rooms > 0)');
            DB::statement('ALTER TABLE kosts ADD CONSTRAINT chk_kosts_available_rooms_range CHECK (available_rooms >= 0 AND available_rooms <= total_rooms)');
            DB::statement('ALTER TABLE kosts ADD FULLTEXT kosts_name_description_fulltext (name, description)');
        }
    }

    public function down(): void
    {
        Schema::dropIfExists('kosts');
    }
};
