package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.models.ReviewPointModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ModelFactoryService;
import com.odde.doughnut.testability.TimeTraveler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.sql.Timestamp;
import java.util.Iterator;

@Controller
public class ReviewController {
    private final CurrentUserFetcher currentUserFetcher;
    private final ModelFactoryService modelFactoryService;


    @Resource(name = "timeTraveler")
    private final TimeTraveler timeTraveler;

    public ReviewController(CurrentUserFetcher currentUserFetcher, ModelFactoryService modelFactoryService, TimeTraveler timeTraveler) {
        this.currentUserFetcher = currentUserFetcher;
        this.modelFactoryService = modelFactoryService;
        this.timeTraveler = timeTraveler;
    }

    @GetMapping("/reviews/initial")
    public String review(Model model) {
        UserModel user = currentUserFetcher.getUser();
        ReviewPointEntity reviewPointEntity = user.getOneInitialReviewPointEntity(timeTraveler.getCurrentUTCTimestamp());
        if (reviewPointEntity == null) {
            return "reviews/initial_done";
        }
        model.addAttribute("reviewPointEntity", reviewPointEntity);
        return "reviews/initial";
    }

    @GetMapping("/reviews/repeat")
    public String repeatReview(Model model) {
        UserModel user = currentUserFetcher.getUser();
        Iterator<ReviewPointEntity> iterator = modelFactoryService.reviewPointRepository.findAllByUserEntityOrderByLastReviewedAt(user.getEntity()).iterator();
        if (iterator.hasNext()) {
            ReviewPointEntity reviewPointEntity = iterator.next();
            model.addAttribute("reviewPointEntity", reviewPointEntity);
            return "reviews/repeat";
        }
        return "reviews/initial_done";
    }

    @PostMapping("/reviews")
    public String create(@Valid ReviewPointEntity reviewPointEntity) {
        UserModel userModel = currentUserFetcher.getUser();
        ReviewPointModel reviewPointModel = modelFactoryService.toReviewPointModel(reviewPointEntity);
        reviewPointModel.initalReview(userModel, timeTraveler.getCurrentUTCTimestamp());

        return "redirect:/reviews/initial";
    }

    @PostMapping("/reviews/{reviewPointEntity}")
    public String update(@Valid ReviewPointEntity reviewPointEntity) {
        reviewPointEntity.setLastReviewedAt(timeTraveler.getCurrentUTCTimestamp());
        modelFactoryService.reviewPointRepository.save(reviewPointEntity);
        return "redirect:/reviews/initial";
    }

}
