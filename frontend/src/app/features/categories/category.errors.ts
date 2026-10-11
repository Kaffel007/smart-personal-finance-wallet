import { HttpErrorResponse } from '@angular/common/http';

export function categoryErrorMessage(error: unknown, fallback: string): string {
  if (!(error instanceof HttpErrorResponse)) return fallback;
  if (error.status === 404) return 'Cette catégorie est introuvable. Actualisez la liste.';
  if (error.status === 409) {
    // Map only known backend messages; never expose arbitrary technical content.
    if (error.error?.message === 'Cette catégorie est utilisée par des données financières.') {
      return 'Cette catégorie est utilisée et ne peut pas être supprimée ou changer de type. Son nom peut être modifié.';
    }
    if (error.error?.message === 'Cette catégorie existe déjà.') return 'Une catégorie de ce nom et de ce type existe déjà.';
    return 'Cette action entre en conflit avec les données existantes. Actualisez la liste.';
  }
  if (error.status === 400) return 'Vérifiez le nom et le type de la catégorie.';
  return fallback;
}
