// models/recommendation.interface.ts
export interface TaskRecommendation {
    id: number;
    title: string;
    description: string;
    recommendationScore: number;
    recommendedBy: RecommendationType;
    recommendationReason: string;
    recommended: boolean;
    lastRecommendedOn: string;
}

export type RecommendationType = 'SIMILAR_CONTENT' | 'POPULAR' | 'WEEKDAY_MATCH';

export interface RecommendationResponse {
    status: string;
    message: string;
    recommendations: TaskRecommendation[];
}
